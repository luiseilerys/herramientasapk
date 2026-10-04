package com.apkutils.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * apk-utilities en Android: integra todas las herramientas del repo virb3/apk-utilities
 * (apktool, baksmali/smali, uber-apk-signer, APKEditor, enjarify, aapt, d8) ejecutandolas
 * 100% en el dispositivo mediante un runtime bash/java/python embebido.
 * Las funciones adb-pull/adb-install se reemplazan por equivalentes on-device
 * (copiado directo de /data/app + Intent de instalacion).
 */
public class MainActivity extends AppCompatActivity {

    private static final String PREFS = "apkutil";
    private static final String PREF_SETUP = "setup_done_v2";

    private TextView output;
    private ScrollView scroll;
    private TextView statusLine;
    private EditText termInput;
    private ExecutorService exec;
    private final Map<String, List<String>> mergePicks = new HashMap<>();
    private boolean terminalMode = false;

    private ActivityResultLauncher<Intent> importLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        exec = Executors.newSingleThreadExecutor();
        output = findViewById(R.id.outputText);
        scroll = findViewById(R.id.outputScroll);
        statusLine = findViewById(R.id.statusLine);
        termInput = findViewById(R.id.terminalInput);

        importLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), r -> {
                    if (r.getResultCode() == Activity.RESULT_OK && r.getData() != null
                            && r.getData().getData() != null) {
                        importUri(r.getData().getData());
                    }
                });

        bind(R.id.btnAaptDump, "aapt-dump.sh");
        bind(R.id.btnDecode, "apktool-decode.sh");
        bind(R.id.btnBuild, "apktool-build.sh");
        bind(R.id.btnSign, "sign.sh");
        bind(R.id.btnBaksmali, "baksmali.sh");
        bind(R.id.btnSmali, "smali.sh");
        bind(R.id.btnEnjarify, "enjarify.sh");
        bind(R.id.btnDexify, "dexify.sh");
        bind(R.id.btnPull, "pull.sh");
        bind(R.id.btnInstall, "install.sh");
        bind(R.id.btnLpPull, "lp-pull.sh");
        bind(R.id.btnLpPush, "lp-push.sh");
        bind(R.id.btnClean, "clean.sh");

        Button merge = findViewById(R.id.btnMerge);
        merge.setOnClickListener(v -> showMergeDialog());

        findViewById(R.id.btnImport).setOnClickListener(v -> openImporter());
        Button term = findViewById(R.id.btnTerminal);
        term.setOnClickListener(v -> toggleTerminal());

        termInput.setOnEditorActionListener((tv, actionId, ev) -> {
            if (actionId == EditorInfo.IME_ACTION_GO
                    || actionId == EditorInfo.IME_ACTION_DONE) {
                String cmd = tv.getText().toString();
                tv.setText("");
                runCommand(cmd);
                return true;
            }
            return false;
        });

        ensureStorage();

        boolean done = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(PREF_SETUP, false);
        if (!done) {
            append("Instalando runtime por primera vez (bash, java, python, busybox)... "
                    + "tarda unos minutos.\n");
            exec.execute(() -> {
                try {
                    bootstrapRuntime();
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                            .putBoolean(PREF_SETUP, true).apply();
                    uiAppend("\nRUNTIME LISTO. Pulsa una herramienta o IMPORT.\n");
                } catch (Exception e) {
                    uiAppend("\nERROR de instalacion: " + e + "\n");
                }
            });
        } else {
            append("Runtime instalado. Pulsa una herramienta o IMPORT.\n");
        }
    }

    // ---------------------------------------------------------------- UI

    private void bind(int id, String script) {
        findViewById(id).setOnClickListener(v -> runCommand(script));
    }

    private void toggleTerminal() {
        terminalMode = !terminalMode;
        termInput.setVisibility(terminalMode ? VISIBLE : GONE);
        statusLine.setText(terminalMode
                ? "MODO TERMINAL: escribe comandos bash y pulsa GO"
                : "Modo herramientas.");
    }

    private void runCommand(String cmd) {
        if (TextUtils.isEmpty(cmd)) return;
        append("\n$ " + cmd + "\n");
        exec.execute(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(shellPath(), "-lc", cmd);
                pb.directory(homeDir());
                env(pb.environment());
                pb.redirectErrorStream(true);
                Process p = pb.start();
                drain(p.getInputStream());
                int rc = p.waitFor();
                uiAppend("[exit " + rc + "]");
            } catch (IOException e) {
                uiAppend("spawn error: " + e.getMessage());
            }
        });
    }

    private File homeDir() {
        File f = new File(getFilesDir(), "home");
        if (!f.exists()) f.mkdirs();
        return f;
    }

    private File binDir() {
        File f = new File(getFilesDir(), "bin");
        if (!f.exists()) f.mkdirs();
        return f;
    }

    private File assetsRoot() {
        File f = new File(getFilesDir(), "assets");
        if (!f.exists()) f.mkdirs();
        return f;
    }

    private String shellPath() {
        return new File(binDir(), "bash").getAbsolutePath();
    }

    private void env(Map<String, String> e) {
        String home = homeDir().getAbsolutePath();
        String bin = binDir().getAbsolutePath();
        String as = assetsRoot().getAbsolutePath();
        e.put("APKU_HOME", home);
        e.put("APKU_BIN", bin);
        e.put("APKU_ASSETS", as);
        e.put("APKU_CONFIG", new File(as, "shell_config.sh").getAbsolutePath());
        String jvm = new File(home, "usr/lib/jvm/java-17-openjdk").getAbsolutePath();
        e.put("APKU_JAVA", jvm);
        e.put("JAVA_HOME", jvm);
        e.put("HOME", home);
        e.put("PATH", bin + ":/system/bin:/system/xbin");
        e.put("LD_LIBRARY_PATH", new File(home, "usr/lib").getAbsolutePath());
    }

    private void append(String s) {
        output.append(s);
        scroll.post(() -> scroll.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private void uiAppend(String s) {
        runOnUiThread(() -> append("\n" + s + "\n"));
    }

    private void drain(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        int total = 0;
        while ((line = br.readLine()) != null) {
            sb.append(line).append('\n');
            total += line.length() + 1;
            if (total > 60000) {
                final String chunk = sb.toString();
                runOnUiThread(() -> append(chunk));
                sb.setLength(0);
                total = 0;
            }
        }
        final String rest = sb.toString();
        runOnUiThread(() -> append(rest));
    }

    // ------------------------------------------------------------- import

    private void openImporter() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        importLauncher.launch(i);
    }

    private void importUri(Uri uri) {
        exec.execute(() -> {
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                String name = queryName(uri);
                if (name == null) name = "imported-" + System.currentTimeMillis() + ".apk";
                File dir = new File(homeDir(), "project");
                dir.mkdirs();
                File out = new File(dir, name);
                try (FileOutputStream fos = new FileOutputStream(out)) {
                    byte[] buf = new byte[65536];
                    int n;
                    while ((n = in.read(buf)) > 0) fos.write(buf, 0, n);
                }
                uiAppend("Importado: project/" + name + " (" + out.length() + " bytes)");
            } catch (IOException e) {
                uiAppend("Import fallo: " + e.getMessage());
            }
        });
    }

    private String queryName(Uri uri) {
        try (android.database.Cursor c = getContentResolver().query(
                uri, new String[]{android.provider.OpenableColumns.DISPLAY_NAME},
                null, null, null)) {
            if (c != null && c.moveToFirst()) {
                return c.getString(0);
            }
        } catch (Exception ignored) {}
        return null;
    }

    // -------------------------------------------------------------- merge

    private void showMergeDialog() {
        File dir = new File(homeDir(), "project");
        File[] apks = dir.listFiles((d, n) -> n.endsWith(".apk"));
        if (apks == null || apks.length < 2) {
            Toast.makeText(this,
                    "merge necesita >=2 split APKs en project/ (usa pull primero)",
                    Toast.LENGTH_LONG).show();
            return;
        }
        String[] names = new String[apks.length];
        boolean[] checked = new boolean[apks.length];
        for (int i = 0; i < apks.length; i++) {
            names[i] = apks[i].getName();
            checked[i] = mergePicks
                    .computeIfAbsent("m", k -> new ArrayList<>()).contains(names[i]);
        }
        new android.app.AlertDialog.Builder(this)
                .setTitle("Selecciona splits a unir")
                .setMultiChoiceItems(names, checked, (d, which, isChecked) ->
                        checked[which] = isChecked)
                .setPositiveButton("Unir", (d, w) -> {
                    StringBuilder args = new StringBuilder();
                    for (int i = 0; i < names.length; i++) {
                        if (checked[i]) args.append('\'')
                                .append(names[i].replace("'", "'\\''"))
                                .append("' ");
                    }
                    if (args.length() > 0) runCommand("merge.sh " + args);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ------------------------------------------------------------ storage

    private void ensureStorage() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                new android.app.AlertDialog.Builder(this)
                        .setTitle("Permisos de almacenamiento")
                        .setMessage("apk-utilities necesita acceso completo a "
                                + "archivos para leer /data/app y Lucky Patcher.")
                        .setPositiveButton("Abrir ajustes", (d, w) -> {
                            try {
                                startActivity(new Intent(
                                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                        Uri.parse("package:" + getPackageName())));
                            } catch (Exception e) {
                                startActivity(new Intent(
                                        Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
                            }
                        })
                        .setNegativeButton("Luego", null)
                        .show();
            }
        } else if (android.os.Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        }
    }

    // ----------------------------------------------------------- bootstrap

    /**
     * Fase 1: copia scripts/config/jars/python/templates a files/assets.
     * Fase 2: extrae el .deb de busybox y usa su `tar` para desplegar el resto
     *         de .debs (bash, coreutils, java, python...) sobre files/home.
     */
    private void bootstrapRuntime() throws IOException, InterruptedException {
        File home = homeDir();
        File bin = binDir();
        File as = assetsRoot();
        home.mkdirs(); bin.mkdirs(); as.mkdirs();

        copyAssetTree("sh_scripts", as);
        copyAssetTree("sh_templates", as);
        copyAssetTree("enjarify", as);
        for (String j : new String[]{"apktool_2.12.0.jar", "baksmali-2.5.2.jar",
                "smali-2.5.2.jar", "uber-apk-signer-1.3.0.jar", "APKEditor-1.4.5.jar"}) {
            copyOne(j, new File(as, j));
        }
        copyOne("shell_config.sh", new File(as, "shell_config.sh"));

        // --- busybox como motor de extraccion ---
        File bbDir = new File(home, "bb");
        bbDir.mkdirs();
        extractArchive("deb/busybox_1.38.0-1_aarch64.deb", home, true);
        File bb = new File(bbDir, "usr/bin/busybox");
        if (!bb.canExecute()) bb.setExecutable(true, false);
        if (!bb.isFile()) throw new IOException("busybox no extraido");
        File bbTar = new File(bbDir, "usr/bin/tar");
        if (!bbTar.isFile()) {
            try {
                java.nio.file.Files.createSymbolicLink(bbTar.toPath(),
                        bb.toPath().getFileName());
            } catch (Exception e) {
                copyOneFile(bb, bbTar);
            }
            bbTar.setExecutable(true, false);
        }

        // --- resto de paquetes (.deb y tar.xz del JRE minimo) ---
        String[] pkgs = getAssets().list("deb");
        java.util.Arrays.sort(pkgs);
        for (String d : pkgs) {
            if (d.startsWith("busybox_")) continue;
            uiAppend("Desplegando " + d + " ...");
            extractArchive("deb/" + d, home, d.endsWith(".deb"));
        }
        // re-extraer busybox por si algun .so compartido lo necesita
        extractArchive("deb/busybox_1.38.0-1_aarch64.deb", home, true);

        // --- enlaces: usr/bin -> files/bin (PATH de la app) ---
        File usrBin = new File(home, "usr/bin");
        if (usrBin.isDirectory()) {
            for (File f : usrBin.listFiles()) {
                File link = new File(bin, f.getName());
                if (!link.exists()) {
                    try {
                        java.nio.file.Files.createSymbolicLink(
                                link.toPath(), f.toPath().getFileName());
                    } catch (Exception e) {
                        // sin symlinks: copiamos el launcher
                    }
                }
            }
        }
        // LD_LIBRARY_PATH apunta a home/usr/lib
        chmodAll(bin);
        chmodAll(usrBin);
        chmodAll(new File(home, "usr/lib/jvm"));

        // wrapper `python` -> python3
        File py = new File(bin, "python");
        if (!py.exists() && new File(bin, "python3").exists()) {
            writeScript(py, "#!/bin/sh\nexec \"" + new File(bin, "python3")
                    .getAbsolutePath() + "\" \"$@\"\n");
        }
        // wrappers de herramientas (nombre simple -> script .sh)
        String[][] tools = {
                {"aapt-dump", "aapt-dump.sh"}, {"decode", "apktool-decode.sh"},
                {"build", "apktool-build.sh"}, {"sign", "sign.sh"},
                {"baksmali", "baksmali.sh"}, {"smali", "smali.sh"},
                {"enjarify", "enjarify.sh"}, {"dexify", "dexify.sh"},
                {"merge", "merge.sh"}, {"pull", "pull.sh"},
                {"install", "install.sh"}, {"lp-pull", "lp-pull.sh"},
                {"lp-push", "lp-push.sh"}, {"clean", "clean.sh"},
                {"edit", "edit.sh"},
        };
        File scripts = new File(as, "sh_scripts");
        for (String[] t : tools) {
            writeScript(new File(bin, t[0]),
                    "#!/bin/bash\nexec bash '" + new File(scripts, t[1])
                            .getAbsolutePath() + "' \"$@\"\n");
        }
        writeScript(new File(bin, "menu"),
                "#!/bin/bash\n. '" + new File(as, "shell_config.sh")
                        .getAbsolutePath() + "'\nmenu\n");

        // verificar java embebido
        File javaBin = new File(home, "usr/lib/jvm/java-17-openjdk/bin/java");
        if (!javaBin.isFile()) {
            uiAppend("AVISO: no se encontro java embebido (" + javaBin + ")");
        } else {
            javaBin.setExecutable(true, false);
        }
        uiAppend("Bootstrap completado.");
    }

    private void copyAssetTree(String path, File destRoot)
            throws IOException {
        String[] entries = safeList(path);
        if (entries == null || entries.length == 0) return;
        for (String name : entries) {
            String assetPath = path + "/" + name;
            String[] sub = safeList(assetPath);
            File dest = new File(destRoot, assetPath);
            if (sub != null && sub.length > 0) {
                dest.mkdirs();
                copyAssetTree(assetPath, destRoot);
            } else {
                copyOne(assetPath, dest);
            }
        }
    }

    private String[] safeList(String p) {
        try {
            return getAssets().list(p);
        } catch (IOException e) {
            return null;
        }
    }

    private void copyOne(String assetPath, File dest) throws IOException {
        dest.getParentFile().mkdirs();
        try (InputStream in = getAssets().open(assetPath);
             FileOutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
    }

    private void copyOneFile(File src, File dst) throws IOException {
        dst.getParentFile().mkdirs();
        try (InputStream in = new java.io.FileInputStream(src);
             FileOutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
    }

    /**
     * Extrae un paquete de assets hacia home.
     *  - isDeb=true : es un .deb (contenedor ar) -> saca data.tar.xz y lo descomprime
     *  - isDeb=false: es un .tar.xz plano -> se descomprime directamente
     * Usa busybox/tar una vez disponible; antes recurre al `tar` de Android.
     */
    private void extractArchive(String assetPath, File home, boolean isDeb)
            throws IOException, InterruptedException {
        File tmp = new File(getCacheDir(), "pkg.tmp");
        copyOne(assetPath, tmp);
        File payload = tmp;
        if (isDeb) {
            payload = new File(getCacheDir(), "data.tar.xz");
            extractArMember(tmp, "data.tar.xz", payload);
            tmp.delete();
        }
        File bbTar = new File(home, "bb/usr/bin/tar");
        List<String> cmd;
        if (bbTar.canExecute()) {
            cmd = List.of(bbTar.getAbsolutePath(), "-xJf",
                    payload.getAbsolutePath(), "-C", home.getAbsolutePath());
        } else {
            cmd = List.of("tar", "-xJf", payload.getAbsolutePath(),
                    "-C", home.getAbsolutePath());
        }
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.environment().put("TMPDIR", getCacheDir().getAbsolutePath());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        drain(p.getInputStream());
        int rc = p.waitFor();
        payload.delete();
        if (rc != 0) throw new IOException("extraccion fallo rc=" + rc
                + " para " + assetPath);
    }

    /** Lee un archivo ar (debian-binary/control.tar/data.tar...) y saca un miembro. */
    private void extractArMember(File ar, String member, File out)
            throws IOException {
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(ar, "r");
             FileOutputStream fos = new FileOutputStream(out)) {
            byte[] magic = new byte[8];
            raf.readFully(magic);
            if (!(new String(magic, StandardCharsets.US_ASCII)).equals("!<arch>\n"))
                throw new IOException("no es un .deb/ar: " + ar);
            long pos = 8;
            byte[] hdr = new byte[60];
            while (pos + 60 <= raf.length()) {
                raf.seek(pos);
                raf.readFully(hdr);
                String h = new String(hdr, StandardCharsets.US_ASCII);
                String name = h.substring(0, 16).trim();
                if (name.endsWith("/")) name = name.substring(0, name.length() - 1);
                long size = Long.parseLong(h.substring(48, 58).trim());
                if (name.equals(member)) {
                    raf.seek(pos + 60);
                    byte[] buf = new byte[65536];
                    long left = size;
                    while (left > 0) {
                        int n = raf.read(buf, 0,
                                (int) Math.min(buf.length, left));
                        if (n <= 0) break;
                        fos.write(buf, 0, n);
                        left -= n;
                    }
                    return;
                }
                pos += 60 + size + (size % 2);
            }
            throw new IOException(member + " no encontrado en " + ar.getName());
        }
    }

    private void chmodAll(File dir) {
        if (!dir.isDirectory()) return;
        File[] fs = dir.listFiles();
        if (fs == null) return;
        for (File f : fs) {
            if (f.isDirectory()) chmodAll(f);
            else f.setExecutable(true, false);
        }
    }

    private void writeScript(File f, String body) throws IOException {
        f.getParentFile().mkdirs();
        try (FileWriter fw = new FileWriter(f)) {
            fw.write(body);
        }
        f.setExecutable(true, false);
    }

    private static class FileWriter extends java.io.OutputStreamWriter {
        FileWriter(File f) throws IOException {
            super(new FileOutputStream(f), StandardCharsets.UTF_8);
        }
    }
}
