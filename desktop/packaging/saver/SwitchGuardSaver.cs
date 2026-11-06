// SwitchGuard ekran koruyucusu (Windows .scr). Windows boşta kalınca "/s" ile çalıştırır.
// Kendisi bir şey çizmez: kurulu SwitchGuard'a "--saver" ile bekleme ekranını açtırır ve bekleme ekranı
// kapanana kadar (fare/klavye) bekler. Ekran koruyucu süreci bitince Windows, ayarlıysa oturumu kilitler.
// Derleme: desktop/build.gradle.kts → compileSaver (.NET Framework csc, her Windows 10/11'de var).
// SwitchGuard.exe'nin yolu, ayarı açarken uygulamanın yazdığı %APPDATA%\SwitchGuard\saver-app.txt'den okunur.
using System;
using System.Diagnostics;
using System.IO;
using System.Threading;

[assembly: System.Reflection.AssemblyTitle("SwitchGuard")]
[assembly: System.Reflection.AssemblyProduct("SwitchGuard")]

static class SwitchGuardSaver
{
    static int Main(string[] args)
    {
        string mode = args.Length > 0 ? args[0].Trim().ToLowerInvariant() : "/c";
        if (mode.StartsWith("/p")) return 0; // Ayarlar'daki küçük önizleme: çizmiyoruz.
        string dir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "SwitchGuard");
        string exe = null;
        try { exe = File.ReadAllText(Path.Combine(dir, "saver-app.txt")).Trim(); } catch { }
        if (string.IsNullOrEmpty(exe) || !File.Exists(exe)) return 1;

        if (!mode.StartsWith("/s"))
        {
            // "/c" (Ayarlar düğmesi) ya da çift tıklama: uygulamayı aç.
            Process.Start(new ProcessStartInfo(exe) { UseShellExecute = false });
            return 0;
        }

        string marker = Path.Combine(dir, "saver.open");
        Process.Start(new ProcessStartInfo(exe, "--saver") { UseShellExecute = false });
        // Bekleme ekranının açılmasını bekle (uygulama kapalıysa açılışı birkaç saniye sürer).
        DateTime until = DateTime.UtcNow.AddSeconds(45);
        while (!File.Exists(marker))
        {
            if (DateTime.UtcNow > until) return 0;
            Thread.Sleep(250);
        }
        // Kapanana kadar bekle; uygulama çökerse işaret kalabilir: dosya her 30 sn'de bir tazelenir, eskiyse bitir.
        while (File.Exists(marker))
        {
            try
            {
                if ((DateTime.UtcNow - File.GetLastWriteTimeUtc(marker)).TotalSeconds > 90) break;
            }
            catch { }
            Thread.Sleep(300);
        }
        return 0;
    }
}
