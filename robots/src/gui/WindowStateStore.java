package gui;

import java.awt.Rectangle;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;

final class WindowStateStore
{
    private static final String CONFIG_FILE = ".robots.config";

    private WindowStateStore()
    {
    }

    static void save(JFrame mainFrame, JDesktopPane desktopPane) throws IOException
    {
        Properties properties = new Properties();
        Rectangle bounds = mainFrame.getBounds();
        int extendedState = mainFrame.getExtendedState();
        saveBounds(properties, "main", bounds);
        properties.setProperty("main.extendedState", Integer.toString(extendedState));

        for (JInternalFrame frame : desktopPane.getAllFrames())
        {
            String prefix = internalFramePrefix(frame);
            saveBounds(properties, prefix, frame.getBounds());
            properties.setProperty(prefix + ".iconified", Boolean.toString(frame.isIcon()));
            properties.setProperty(prefix + ".maximized", Boolean.toString(frame.isMaximum()));
        }

        File configFile = getConfigFile();
        try (FileOutputStream output = new FileOutputStream(configFile))
        {
            properties.store(output, "Robots window state");
        }
    }

    static boolean restore(JFrame mainFrame, JDesktopPane desktopPane)
        throws IOException, PropertyVetoException
    {
        Properties properties = new Properties();
        File configFile = getConfigFile();
        if (!configFile.isFile())
        {
            return false;
        }
        try (FileInputStream input = new FileInputStream(configFile))
        {
            properties.load(input);
        }

        WindowState mainState = readMainState(properties);
        if (mainState == null)
        {
            return false;
        }

        Map<JInternalFrame, WindowState> internalStates = new HashMap<>();
        for (JInternalFrame frame : desktopPane.getAllFrames())
        {
            WindowState state = readInternalState(properties, internalFramePrefix(frame));
            if (state != null)
            {
                internalStates.put(frame, state);
            }
        }

        mainFrame.setBounds(mainState.bounds);
        mainFrame.setExtendedState(mainState.extendedState);
        for (Map.Entry<JInternalFrame, WindowState> entry : internalStates.entrySet())
        {
            JInternalFrame frame = entry.getKey();
            WindowState state = entry.getValue();
            frame.setBounds(state.bounds);
            if (state.maximized)
            {
                frame.setMaximum(true);
            }
            if (state.iconified)
            {
                frame.setIcon(true);
            }
        }
        return true;
    }

    private static WindowState readMainState(Properties properties)
    {
        Rectangle bounds = readBounds(properties, "main");
        if (bounds == null || !properties.containsKey("main.extendedState"))
        {
            return null;
        }
        return new WindowState(bounds,
            Integer.parseInt(properties.getProperty("main.extendedState")), false, false);
    }

    private static WindowState readInternalState(Properties properties, String prefix)
    {
        Rectangle bounds = readBounds(properties, prefix);
        if (bounds == null || !properties.containsKey(prefix + ".iconified")
            || !properties.containsKey(prefix + ".maximized"))
        {
            return null;
        }
        return new WindowState(bounds, 0,
            Boolean.parseBoolean(properties.getProperty(prefix + ".iconified")),
            Boolean.parseBoolean(properties.getProperty(prefix + ".maximized")));
    }

    private static Rectangle readBounds(Properties properties, String prefix)
    {
        String[] names = { ".x", ".y", ".width", ".height" };
        for (String name : names)
        {
            if (!properties.containsKey(prefix + name))
            {
                return null;
            }
        }
        int width = Integer.parseInt(properties.getProperty(prefix + ".width"));
        int height = Integer.parseInt(properties.getProperty(prefix + ".height"));
        if (width <= 0 || height <= 0)
        {
            throw new IllegalArgumentException("Invalid window size in configuration");
        }
        return new Rectangle(
            Integer.parseInt(properties.getProperty(prefix + ".x")),
            Integer.parseInt(properties.getProperty(prefix + ".y")),
            width,
            height);
    }

    private static void saveBounds(Properties properties, String prefix, Rectangle bounds)
    {
        properties.setProperty(prefix + ".x", Integer.toString(bounds.x));
        properties.setProperty(prefix + ".y", Integer.toString(bounds.y));
        properties.setProperty(prefix + ".width", Integer.toString(bounds.width));
        properties.setProperty(prefix + ".height", Integer.toString(bounds.height));
    }

    private static String internalFramePrefix(JInternalFrame frame)
    {
        return "internal." + frame.getTitle();
    }

    private static File getConfigFile() throws IOException
    {
        String homeDirectory = System.getProperty("user.home");
        if (homeDirectory == null || homeDirectory.isEmpty())
        {
            throw new IOException("User home directory is not available");
        }
        return new File(homeDirectory, CONFIG_FILE);
    }

    private static final class WindowState
    {
        private final Rectangle bounds;
        private final int extendedState;
        private final boolean iconified;
        private final boolean maximized;

        private WindowState(Rectangle bounds, int extendedState,
            boolean iconified, boolean maximized)
        {
            this.bounds = bounds;
            this.extendedState = extendedState;
            this.iconified = iconified;
            this.maximized = maximized;
        }
    }
}
