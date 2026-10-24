const { app, BrowserWindow, shell } = require("electron");
const path = require("path");
function create() {
  const win = new BrowserWindow({
    width: 1280, height: 800, show: false, autoHideMenuBar: true,
    backgroundColor: "#0f0f12", icon: path.join(__dirname, "icon.png"),
    webPreferences: { contextIsolation: true, nodeIntegration: false },
  });
  win.setMenu(null);
  win.loadFile(path.join(__dirname, "splash.html"));
  win.once("ready-to-show", () => { win.maximize(); win.show(); });
  setTimeout(() => win.loadURL("https://truestream-video-village.lovable.app"), 2000);
  win.webContents.setWindowOpenHandler(({ url }) => { shell.openExternal(url); return { action: "deny" }; });
}
app.whenReady().then(create);
app.on("window-all-closed", () => app.quit());
