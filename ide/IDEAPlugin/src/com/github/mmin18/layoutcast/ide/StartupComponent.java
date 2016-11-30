package com.github.mmin18.layoutcast.ide;

import com.android.builder.model.AndroidProject;
import com.android.tools.idea.gradle.facet.AndroidGradleFacet;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.PathUtil;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;

import java.io.*;

import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by mmin18 on 7/29/15.
 */
public class StartupComponent implements ApplicationComponent {
    private static File UPDATE_FILE;
    private static byte[] UPDATE_DATA;
    private static double UPDATE_VER;
    private static File STROKE_FILE;
    private static double STROKE_VER;

    private static ConsoleView consoleView;


    public StartupComponent() {
    }

    public void initComponent() {

        new Thread() {
            @Override
            public void run() {
                try {
                    URL url;
                    if (TimeZone.getDefault().getRawOffset() == 8 * 3600 * 1000) {
                        // +8000 is probably china
                        url = new URL("http://7xkzs8.com1.z0.glb.clouddn.com/cast.py");
                    } else {
                        url = new URL("https://github.com/xfile1121/LayoutCast/blob/master/cast.py");
                    }
                    InputStream ins = url.openConnection().getInputStream();
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buf = new byte[4096];
                    int l;
                    while ((l = ins.read(buf)) != -1) {
                        bos.write(buf, 0, l);
                    }
                    ins.close();
                    bos.close();
                    byte[] data = bos.toByteArray();
                    String str = new String(data, "utf-8");
                    Pattern p = Pattern.compile("^__plugin__\\s*=\\s*['\"](\\d+)['\"]", Pattern.MULTILINE);
                    Matcher m = p.matcher(str);
                    if (m.find()) {
                        String s = m.group(1);
                        if ("1".equals(s)) {
                            ByteArrayInputStream bis = new ByteArrayInputStream(data);
                            UPDATE_VER = CastAction.readVersion(bis);
                            bis.close();
                            UPDATE_DATA = data;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();

//        AndroidFacet.getInstance()
    }

    public void disposeComponent() {
    }

    @NotNull
    public String getComponentName() {
        return "StartupComponent";
    }


    public static double getCastVersion() {
        if (STROKE_VER == 0) {
            try {
                File cp = new File(PathUtil.getJarPathForClass(StartupComponent.class));
                if (cp.isDirectory()) {
                    File file = new File(cp, "cast.py");
                    InputStream ins = new FileInputStream(file);
                    STROKE_VER = CastAction.readVersion(ins);
                    ins.close();
                } else {
                    ZipFile zf = new ZipFile(cp);
                    ZipEntry ze = zf.getEntry("cast.py");
                    InputStream ins = zf.getInputStream(ze);
                    STROKE_VER = CastAction.readVersion(ins);
                    ins.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return Math.max(STROKE_VER, UPDATE_VER);
    }

    public static File getCastFile() {
        getCastVersion();
        if (STROKE_VER > UPDATE_VER) {
            if (STROKE_FILE == null) {
                try {
                    File cp = new File(PathUtil.getJarPathForClass(StartupComponent.class));
                    if (cp.isDirectory()) {
                        STROKE_FILE = new File(cp, "cast.py");
                    } else {
                        ZipFile zf = new ZipFile(cp);
                        ZipEntry ze = zf.getEntry("cast.py");
                        InputStream ins = zf.getInputStream(ze);
                        File tmp = File.createTempFile("lcast", "" + STROKE_VER);
                        FileOutputStream fos = new FileOutputStream(tmp);
                        ins = zf.getInputStream(ze);
                        byte[] buf = new byte[4096];
                        int l;
                        while ((l = ins.read(buf)) != -1) {
                            fos.write(buf, 0, l);
                        }
                        fos.close();
                        ins.close();
                        STROKE_FILE = tmp;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return STROKE_FILE;
        } else {
            if (UPDATE_FILE == null) {
                try {
                    File tmp = File.createTempFile("lcast", "" + UPDATE_VER);
                    FileOutputStream fos = new FileOutputStream(tmp);
                    fos.write(UPDATE_DATA);
                    fos.close();
                    UPDATE_FILE = tmp;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return UPDATE_FILE;
        }
    }

    public static void printLog(Project project, String log) {
        if(consoleView == null) {
            consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project)
                    .getConsole();

            ToolWindowManager manager = ToolWindowManager.getInstance(project);
            manager.registerToolWindow("LayoutCast Error Log", consoleView.getComponent(), ToolWindowAnchor.BOTTOM);
        }
        consoleView.print(log, ConsoleViewContentType.ERROR_OUTPUT);
    }
}
