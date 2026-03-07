package mediathek.tool.sender_icon_cache;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import mediathek.config.Konstanten;
import mediathek.tool.http.MVHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

class SenderIconCacheLoader {
    private static final String WIKI_BASE_URL = "https://upload.wikimedia.org/wikipedia/commons/thumb";
    private static final Logger logger = LogManager.getLogger();
    private final AtomicBoolean useLocalIcons;

    public SenderIconCacheLoader(@NotNull AtomicBoolean useLocalIcons) {
        this.useLocalIcons = useLocalIcons;
    }

    private @Nullable ImageIcon getLocalImageIcon(@NotNull String source) {
        var url = SenderIconCacheLoader.class.getResource(source);
        if (url != null) {
            return new ImageIcon(url);
        } else
            return null;
    }

    private @Nullable ImageIcon getBundledSenderIcon(@NotNull String sender) {
        String resource = switch (sender.toLowerCase(Locale.ROOT)) {
            case "3sat" -> "/icons/sender/3sat.svg";
            case "ard", "das erste" -> "/icons/sender/ard.svg";
            case "ard-alpha" -> "/icons/sender/ard-alpha.svg";
            case "arte.de", "arte.en", "arte.es", "arte.fr", "arte.it", "arte.pl", "arte" -> "/icons/sender/arte.svg";
            case "br" -> "/icons/sender/br.svg";
            case "hr" -> "/icons/sender/hr.svg";
            case "kika" -> "/icons/sender/kika.svg";
            case "mdr" -> "/icons/sender/mdr.svg";
            case "ndr" -> "/icons/sender/ndr.svg";
            case "one" -> "/icons/sender/one.svg";
            case "phoenix" -> "/icons/sender/phoenix.svg";
            case "radio bremen tv", "radio bremen" -> "/icons/sender/radio-bremen.svg";
            case "rbb" -> "/icons/sender/rbb.svg";
            case "sr" -> "/icons/sender/sr.svg";
            case "swr" -> "/icons/sender/swr.svg";
            case "tagesschau24" -> "/icons/sender/tagesschau24.svg";
            case "wdr" -> "/icons/sender/wdr.svg";
            case "zdf" -> "/icons/sender/zdf.svg";
            case "zdfinfo" -> "/icons/sender/ZDFinfo.svg";
            case "zdfneo" -> "/icons/sender/ZDFneo.svg";
            case "deutscher bundestag", "parlamentsfernsehen kanal 1", "parlamentsfernsehen kanal 2" -> "/icons/sender/Deutscher_Bundestag.svg";
            default -> null;
        };

        if (resource == null) {
            return null;
        }

        var url = SenderIconCacheLoader.class.getResource(resource);
        if (url == null) {
            logger.warn("Bundled sender icon resource missing for sender '{}' (resource: {})", sender, resource);
            return null;
        }

        if (resource.endsWith(".svg")) {
            FlatSVGIcon svgIcon = new FlatSVGIcon(url).derive(256, 256);
            BufferedImage rendered = new BufferedImage(svgIcon.getIconWidth(), svgIcon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = rendered.createGraphics();
            try {
                svgIcon.paintIcon(null, g2, 0, 0);
            } finally {
                g2.dispose();
            }

            return new ImageIcon(trimTransparentBorder(rendered));
        }

        return new ImageIcon(url);
    }

    private static @NotNull BufferedImage trimTransparentBorder(@NotNull BufferedImage image) {
        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = (image.getRGB(x, y) >>> 24) & 0xFF;
                if (alpha != 0) {
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return image;
        }

        int width = (maxX - minX) + 1;
        int height = (maxY - minY) + 1;
        BufferedImage cropped = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = cropped.createGraphics();
        try {
            g2.drawImage(image, 0, 0, width, height, minX, minY, maxX + 1, maxY + 1, null);
        } finally {
            g2.dispose();
        }
        return cropped;
    }

    /**
     * Download an icon from network or use local resourcec
     *
     * @param networkResource network address to image
     * @param localResource   resource address
     * @return the scaled image
     */
    private @Nullable ImageIcon getIcon(@NotNull String sender, @NotNull String networkResource, @NotNull String localResource) {
        ImageIcon icon = null;
        final ImageIcon bundledSenderIcon = getBundledSenderIcon(sender);

        if (!useLocalIcons.get()) {
            var userAgent = String.format("%s %s", Konstanten.PROGRAMMNAME,Konstanten.MVVERSION);
            final Request request = new Request.Builder()
                    .url(networkResource)
                    .get()
                    .header("User-Agent", userAgent)
                    .build();

            try (Response response = MVHttpClient.getInstance().getHttpClient().newCall(request).execute();
                 ResponseBody body = response.body()) {
                if (response.isSuccessful()) {
                    BufferedImage b_img = ImageIO.read(body.byteStream());
                    icon = new ImageIcon(b_img);
                } else {
                    logger.warn("Wiki icon request failed for sender '{}' (url: {}, status: {})",
                            sender, networkResource, response.code());
                }
            } catch (Exception ex) {
                logger.warn("Wiki icon request failed for sender '{}' (url: {})",
                        sender, networkResource, ex);
                icon = null;
            }
        }

        //if network is unreachable we get an image with size -1...
        if (icon == null || icon.getIconWidth() < 0 || icon.getIconHeight() < 0) {
            if (bundledSenderIcon != null) {
                icon = bundledSenderIcon;
            } else {
                var url = SenderIconCacheLoader.class.getResource(localResource);
                if (url != null)
                    icon = new ImageIcon(url);
                else {
                    logger.error("Could not load icon from local jar for sender '{}' (resource: {}, bundled: none)",
                            sender, localResource);
                    icon = null;
                }
            }
        }

        if (!useLocalIcons.get() && icon == null) {
            logger.warn("Wiki icon result is empty for sender '{}' (url: {})", sender, networkResource);
        }

        return icon;
    }

    public @NotNull Optional<ImageIcon> load(@NotNull String sender) {
        ImageIcon icon = switch (sender) {
            case "3Sat" -> getIcon("3Sat", WIKI_BASE_URL + "/f/f2/3sat-Logo.svg/775px-3sat-Logo.svg.png", "/mediathek/res/sender/3sat.png");
            case "ARD" -> getIcon("ARD", WIKI_BASE_URL + "/6/68/ARD_logo.svg/320px-ARD_logo.svg.png", "/mediathek/res/sender/ard.png");
            //case "ARD-alpha" -> null;
            case "ARTE.DE" -> getIcon("ARTE.DE", WIKI_BASE_URL + "/0/0e/Arte_Logo_2011.svg/320px-Arte_Logo_2011.svg.png", "/mediathek/res/sender/arte-de.png");
            case "ARTE.EN" -> getIcon("ARTE.EN", WIKI_BASE_URL + "/0/0e/Arte_Logo_2011.svg/320px-Arte_Logo_2011.svg.png", "/mediathek/res/sender/arte-en.png");
            case "ARTE.ES" -> getIcon("ARTE.ES", WIKI_BASE_URL + "/0/0e/Arte_Logo_2011.svg/320px-Arte_Logo_2011.svg.png", "/mediathek/res/sender/arte-es.png");
            case "ARTE.FR" -> getIcon("ARTE.FR", WIKI_BASE_URL + "/0/0e/Arte_Logo_2011.svg/320px-Arte_Logo_2011.svg.png", "/mediathek/res/sender/arte-fr.png");
            case "ARTE.IT" -> getIcon("ARTE.IT", WIKI_BASE_URL + "/0/0e/Arte_Logo_2011.svg/320px-Arte_Logo_2011.svg.png", "/mediathek/res/sender/arte-it.png");
            case "ARTE.PL" -> getIcon("ARTE.PL", WIKI_BASE_URL + "/0/0e/Arte_Logo_2011.svg/320px-Arte_Logo_2011.svg.png", "/mediathek/res/sender/arte-pl.png");
            case "BR" -> getIcon("BR", WIKI_BASE_URL + "/9/98/BR_Dachmarke.svg/320px-BR_Dachmarke.svg.png", "/mediathek/res/sender/br.png");
            case "HR" -> getIcon("HR", WIKI_BASE_URL + "/6/63/HR_Logo.svg/519px-HR_Logo.svg.png", "/mediathek/res/sender/hr.png");
            case "KiKA" -> getIcon("KiKA", WIKI_BASE_URL + "/f/f5/Kika_2012.svg/320px-Kika_2012.svg.png", "/mediathek/res/sender/kika.png");
            case "MDR" -> getIcon("MDR", WIKI_BASE_URL + "/6/61/MDR_Logo_2017.svg/800px-MDR_Logo_2017.svg.png", "/mediathek/res/sender/mdr.png");
            case "DW" -> getIcon("DW", WIKI_BASE_URL + "/6/69/Deutsche_Welle_Logo.svg/743px-Deutsche_Welle_Logo.svg.png", "/mediathek/res/sender/dw.png");
            case "NDR" -> getIcon("NDR", WIKI_BASE_URL + "/0/08/NDR_Dachmarke.svg/308px-NDR_Dachmarke.svg.png", "/mediathek/res/sender/ndr.png");
            //case "ONE" -> null;
            //case "tagesschau24" -> null;
            case "ORF" -> getIcon("ORF", WIKI_BASE_URL + "/d/dd/ORF_logo.svg/709px-ORF_logo.svg.png", "/mediathek/res/sender/orf.png");
            case "RBB" -> getIcon("RBB", WIKI_BASE_URL + "/7/79/Rbb_Logo_2017.08.svg/320px-Rbb_Logo_2017.08.svg.png", "/mediathek/res/sender/rbb.png");
            case "SR" -> getIcon("SR", WIKI_BASE_URL + "/8/83/SR_Dachmarke.svg/602px-SR_Dachmarke.svg.png", "/mediathek/res/sender/sr.png");
            case "SRF" -> getIcon("SRF", WIKI_BASE_URL + "/8/84/Schweizer_Radio_und_Fernsehen_Logo.svg/559px-Schweizer_Radio_und_Fernsehen_Logo.svg.png", "/mediathek/res/sender/srf.png");
            //case "SRF.Podcast" -> null;
            case "SWR" -> getIcon("SWR", WIKI_BASE_URL + "/6/6f/SWR_Dachmarke.svg/320px-SWR_Dachmarke.svg.png", "/mediathek/res/sender/swr.png");
            case "WDR" -> getIcon("WDR", WIKI_BASE_URL + "/9/9b/WDR_Dachmarke.svg/320px-WDR_Dachmarke.svg.png", "/mediathek/res/sender/wdr.png");
            case "ZDF" -> getIcon("ZDF", WIKI_BASE_URL + "/c/c1/ZDF_logo.svg/200px-ZDF_logo.svg.png", "/mediathek/res/sender/zdf.png");
            //case "ZDFinfo" -> null;
            //case "ZDFneo" -> null;
            case "ZDF-tivi" -> getLocalImageIcon("/mediathek/res/sender/zdf-tivi.png");
            case "PHOENIX" -> getIcon("PHOENIX", WIKI_BASE_URL + "/d/de/Phoenix_Logo_2018_ohne_Claim.svg/640px-Phoenix_Logo_2018_ohne_Claim.svg.png", "/mediathek/res/sender/phoenix.png");
            case "Funk.net" -> getIcon("Funk.net", WIKI_BASE_URL + "/9/99/Funk_Logo.svg/454px-Funk_Logo.svg.png", "/mediathek/res/sender/funk_net.png");
            case "Radio Bremen TV" -> getIcon("Radio Bremen TV", WIKI_BASE_URL + "/7/73/Logo_Radio_Bremen_TV.svg/320px-Logo_Radio_Bremen_TV.svg.png", "/mediathek/res/sender/rbtv.jpg");
            default -> getBundledSenderIcon(sender);
        };

        final Optional<ImageIcon> optIcon;
        if (icon == null)
            optIcon = Optional.empty();
        else
            optIcon = Optional.of(icon);

        return optIcon;
    }
}
