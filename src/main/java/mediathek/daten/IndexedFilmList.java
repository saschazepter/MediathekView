package mediathek.daten;

import mediathek.config.StandardLocations;
import mediathek.tool.ApplicationConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.NIOFSDirectory;

import java.nio.file.Path;
import java.util.Locale;

public class IndexedFilmList extends ListeFilme {
    private static final Logger logger = LogManager.getLogger();
    private Directory luceneDirectory;
    private DirectoryReader reader;

    public IndexedFilmList() {
        try  {
            var indexPath = StandardLocations.getFilmIndexPath();
            luceneDirectory = createLuceneDirectory(indexPath);
        } catch (Exception ex) {
            logger.error("Creation of Lucene index directory failed!", ex);
        }
    }

    private Directory createLuceneDirectory(Path indexPath) throws Exception {
        var mode = ApplicationConfiguration.getConfiguration()
                .getString(ApplicationConfiguration.LUCENE_DIRECTORY_MODE, "auto")
                .trim()
                .toLowerCase(Locale.ROOT);

        logger.info("Using Lucene directory mode '{}' for index path {}", mode, indexPath);
        return switch (mode) {
            case "mmap" -> new MMapDirectory(indexPath);
            case "niofs" -> new NIOFSDirectory(indexPath);
            case "auto" -> FSDirectory.open(indexPath);
            default -> {
                logger.warn("Unknown Lucene directory mode '{}', falling back to 'auto'", mode);
                yield FSDirectory.open(indexPath);
            }
        };
    }

    public DirectoryReader getReader() {
        return reader;
    }

    public void setReader(DirectoryReader reader) {
        this.reader = reader;
    }

    public Directory getLuceneDirectory() {
        return luceneDirectory;
    }
}
