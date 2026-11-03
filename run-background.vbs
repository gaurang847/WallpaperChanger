' ===================================================
' WallpaperChanger - Background Silent Launcher
' Runs without showing any console window.
' ===================================================

Set objFSO = CreateObject("Scripting.FileSystemObject")
strScriptDir = objFSO.GetParentFolderName(WScript.ScriptFullName)

strJarPath = strScriptDir & "\target\WallpaperChanger-1.0.0.jar"

If Not objFSO.FileExists(strJarPath) Then
    MsgBox "WallpaperChanger JAR not found at: " & vbCrLf & strJarPath & vbCrLf & vbCrLf & "Please run 'run.bat' or 'install.bat' first.", vbCritical, "Wallpaper Changer"
    WScript.Quit 1
End If

Set objShell = CreateObject("WScript.Shell")
strCommand = "javaw -jar """ & strJarPath & """ --background"
objShell.Run strCommand, 0, False
