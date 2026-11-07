// SwitchGuard ekran koruyucusu (Windows .scr). Windows boşta kalınca "/s" ile çalıştırır.
// Kendisi bir şey çizmez: kurulu SwitchGuard'a "--saver" ile bekleme ekranını açtırır ve HEMEN çıkar.
// Neden beklemiyor: Windows ekran koruyucuyu ayrı bir masaüstünde ("Screen-saver") çalıştırır. Uygulamanın penceresi
// normal masaüstündedir; .scr açık kaldıkça ekran siyah kalır ve fare/klavye penceresi olmayan .scr'ye gider (yalnız
// Ctrl+Alt+Del kurtarırdı). .scr çıkınca Windows normal masaüstüne döner; bekleme ekranı (her zaman üstte) orada
// görünür ve fare/klavye ile kendisi kapanır. "Devam edince oturum açma ekranını göster" açıksa Windows bu noktada
// oturumu kilitler (pano kilidin arkasında kalır).
// Uygulama kapalıysa yeni süreç normal masaüstünde (winsta0\default) başlatılır; yoksa ekran koruyucu masaüstünü
// miras alır ve o masaüstü kapanınca pencereleri görünmez olur.
// Derleme: desktop/build.gradle.kts → compileSaver (.NET Framework csc, her Windows 10/11'de var).
// SwitchGuard.exe'nin yolu, ayarı açarken uygulamanın yazdığı %APPDATA%\SwitchGuard\saver-app.txt'den okunur.
using System;
using System.Diagnostics;
using System.IO;
using System.Runtime.InteropServices;

[assembly: System.Reflection.AssemblyTitle("SwitchGuard")]
[assembly: System.Reflection.AssemblyProduct("SwitchGuard")]

static class SwitchGuardSaver
{
    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
    struct STARTUPINFO
    {
        public int cb; public string lpReserved; public string lpDesktop; public string lpTitle;
        public int dwX, dwY, dwXSize, dwYSize, dwXCountChars, dwYCountChars, dwFillAttribute, dwFlags;
        public short wShowWindow, cbReserved2; public IntPtr lpReserved2, hStdInput, hStdOutput, hStdError;
    }

    [StructLayout(LayoutKind.Sequential)]
    struct PROCESS_INFORMATION { public IntPtr hProcess, hThread; public int dwProcessId, dwThreadId; }

    [DllImport("kernel32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
    static extern bool CreateProcessW(string app, string cmd, IntPtr pa, IntPtr ta, bool inherit, int flags,
        IntPtr env, string dir, ref STARTUPINFO si, out PROCESS_INFORMATION pi);

    [DllImport("kernel32.dll")]
    static extern bool CloseHandle(IntPtr h);

    /** [exe]'yi normal masaüstünde başlatır; olmazsa sıradan Process.Start. */
    static void Launch(string exe, string args)
    {
        var si = new STARTUPINFO { cb = Marshal.SizeOf(typeof(STARTUPINFO)), lpDesktop = "winsta0\\default" };
        PROCESS_INFORMATION pi;
        string cmd = "\"" + exe + "\"" + (args.Length > 0 ? " " + args : "");
        if (CreateProcessW(exe, cmd, IntPtr.Zero, IntPtr.Zero, false, 0, IntPtr.Zero, Path.GetDirectoryName(exe), ref si, out pi))
        {
            CloseHandle(pi.hThread);
            CloseHandle(pi.hProcess);
            return;
        }
        try { Process.Start(new ProcessStartInfo(exe, args) { UseShellExecute = false }); } catch { }
    }

    static int Main(string[] args)
    {
        string mode = args.Length > 0 ? args[0].Trim().ToLowerInvariant() : "/c";
        if (mode.StartsWith("/p")) return 0; // Ayarlar'daki küçük önizleme: çizmiyoruz.
        string dir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "SwitchGuard");
        string exe = null;
        try { exe = File.ReadAllText(Path.Combine(dir, "saver-app.txt")).Trim(); } catch { }
        if (string.IsNullOrEmpty(exe) || !File.Exists(exe)) return 1;

        // "/s": bekleme ekranını ekran koruyucu modunda aç; "/c" (Ayarlar düğmesi) ya da çift tıklama: uygulamayı aç.
        Launch(exe, mode.StartsWith("/s") ? "--saver" : "");
        return 0;
    }
}
