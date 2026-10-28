import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.nio.file.Path;

class HoldShift {
    public static void main(String[] args) throws Exception {
        Robot robot = new Robot();
        if (System.getProperty("os.name").startsWith("Mac")) {
            // MC Pilot records its Node launcher; the Minecraft window belongs to its Java child.
            ProcessHandle launcher = ProcessHandle.of(Long.parseLong(args[0])).orElseThrow();
            long pid;
            try (var children = launcher.descendants()) {
                pid = children.filter(child -> child.info().command()
                    .map(command -> Path.of(command).getFileName().toString().equals("java"))
                    .orElse(false)).findFirst().orElseThrow().pid();
            }
            // AppKit can activate the client without System Events automation permission.
            Process focus = new ProcessBuilder("swift", "-e",
                "import AppKit; exit(NSRunningApplication(processIdentifier: " + pid
                    + ")?.activate(options: [.activateIgnoringOtherApps]) == true ? 0 : 1)")
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT).start();
            if (focus.waitFor() != 0) throw new IllegalStateException("Could not focus Minecraft for native Shift input");
            robot.delay(500);
        }
        robot.keyPress(KeyEvent.VK_SHIFT);
        try {
            System.out.println("ready");
            System.out.flush();
            System.in.read();
        } finally {
            robot.keyRelease(KeyEvent.VK_SHIFT);
        }
    }
}
