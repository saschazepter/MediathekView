/*
 * Copyright (c) 2026 derreisende77.
 * This code was developed as part of the MediathekView project https://github.com/mediathekview/MediathekView
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package mediathek.config

import mediathek.tool.dns.IPvPreferenceMode
import picocli.CommandLine

@CommandLine.Command(name = "MediathekView")
object Config {
    @JvmField
    @CommandLine.Parameters(
        index = "0",
        paramLabel = "<Pfad zum Verzeichnis>",
        description = ["Pfad zum Einstellungsverzeichnis für Portablen Betrieb"],
        arity = "0..1",
    )
    var baseFilePath: String? = null

    /**
     * Specify the preferred DNS IP mode for name resolutions. Defaults to IPv4 only.
     */
    @CommandLine.Option(
        names = ["-dpm", "--dns-preference-mode"],
        description = ["Bevorzugtes IP-Protokoll für DNS festlegen"],
    )
    private var dnsIpPreferenceMode = IPvPreferenceMode.IPV4_ONLY

    @CommandLine.Option(
        names = ["-d", "--debug"],
        hidden = true,
        description = ["Debug-Modus aktivieren (FÜR ENTWICKLER)"],
    )
    private var debug = false

    @CommandLine.Option(
        names = ["-dfd", "--disable-flatlaf-decorations"],
        description = ["Deaktiviert unter Linux Window Manager Dekorationen"],
    )
    private var disableFlatLafDecorations = false

    /**
     * Limit the number of used CPUs on Windows.
     */
    @CommandLine.Option(
        names = ["-n", "--num-cpus"],
        hidden = true,
        description = ["Anzahl der genutzen CPU-Kerne festlegen (FÜR ENTWICKLER)"],
    )
    private var numCpus = 0

    /**
     * For development use parameter to enable TRACE output to log env.
     */
    @CommandLine.Option(
        names = ["-e", "--enhanced-logging"],
        description = ["Erweiterten Log-Modus aktivieren"],
    )
    private var enhancedLogging = false

    /**
     * This will install a repaint manager which monitors Swing EDT violations.
     */
    @CommandLine.Option(
        names = ["-s", "--swing-thread-checker"],
        description = ["Swing EDT Thread Repaint Manager installieren (FÜR ENTWICKLER)"],
        hidden = true,
    )
    private var installThreadCheckingRepaintManager = false

    /**
     * Log HTTP traffic to console. By default HttpLoggingInterceptor.Level.BASIC will be used.
     * Configuration can be changed by ApplicationConfiguration.APPLICATION_DEBUG_HTTP_TRAFFIC_TRACE_LEVEL
     */
    @CommandLine.Option(
        names = ["-t", "--debug-http-traffic"],
        hidden = true,
        description = ["Logging für HTTP Traffic aktivieren (FÜR ENTWICKLER)"],
    )
    private var debugHttpTraffic = false

    private var portableMode = false

    @CommandLine.Option(
        names = ["-m", "--maximized"],
        description = ["Programmfenster beim Start maximieren"],
    )
    private var startMaximized = false

    @CommandLine.Option(
        names = ["-h", "--help"],
        usageHelp = true,
        description = ["Hilfe anzeigen"],
    )
    private var helpRequested = false

    @CommandLine.Option(
        names = ["-f", "--disable-file-logging"],
        description = ["Speichern des Log output in Datei deaktivieren"],
    )
    private var fileLoggingDisabled = false

    /**
     * Disable JVM parameter checks on startup.
     */
    @CommandLine.Option(
        names = ["-nj", "--no-jvm-param-checks"],
        description = ["JVM Parameter-Prüfung deaktivieren"],
    )
    private var disableJvmParameterChecks = false

    @CommandLine.Option(
        names = ["-ns", "--no-splash"],
        description = ["Splash-Screen nicht anzeigen"],
    )
    private var disableSplashScreen = false

    @CommandLine.Option(
        names = ["-dq", "--download-quit"],
        description = ["Automatisch downloaden, dann beenden"],
    )
    private var downloadAndQuit = false

    @JvmStatic
    fun isDisableFlatLafDecorations(): Boolean = disableFlatLafDecorations

    @JvmStatic
    fun getDnsIpPreferenceMode(): IPvPreferenceMode = dnsIpPreferenceMode

    @JvmStatic
    fun setDnsIpPreferenceMode(dnsIpPreferenceMode: IPvPreferenceMode) {
        Config.dnsIpPreferenceMode = dnsIpPreferenceMode
    }

    @JvmStatic
    fun shouldDownloadAndQuit(): Boolean = downloadAndQuit

    @JvmStatic
    fun isSplashScreenDisabled(): Boolean = disableSplashScreen

    @JvmStatic
    fun isDisableJvmParameterChecks(): Boolean = disableJvmParameterChecks

    @JvmStatic
    fun isInstallThreadCheckingRepaintManager(): Boolean = installThreadCheckingRepaintManager

    @JvmStatic
    fun getNumCpus(): Int = numCpus

    @JvmStatic
    fun setNumCpus(num: Int) {
        numCpus = num
    }

    @JvmStatic
    fun isPortableMode(): Boolean = portableMode

    @JvmStatic
    fun setPortableMode(portableMode: Boolean) {
        Config.portableMode = portableMode
    }

    @JvmStatic
    fun isEnhancedLoggingEnabled(): Boolean = enhancedLogging

    @JvmStatic
    fun isDebugModeEnabled(): Boolean = debug

    @JvmStatic
    fun isFileLoggingDisabled(): Boolean = fileLoggingDisabled

    @JvmStatic
    fun isStartMaximized(): Boolean = startMaximized

    @JvmStatic
    fun isHttpTrafficDebuggingEnabled(): Boolean = debugHttpTraffic
}
