package mediathek.javafx.bookmark;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import mediathek.daten.DatenFilm;

import java.util.Optional;

/**
 * Bookmark data definition used to store movies
 *
 * @author K. Wich
 * <p>
 * Note: Prepared for Jackson JSON storage
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookmarkData {
    @JsonProperty("seen")
    private final BooleanProperty seen;
    private String url;
    private String sender;
    @JsonIgnore
    private DatenFilm filmdata;
    private String note;

    public BookmarkData() {
        seen = new SimpleBooleanProperty(false);
    }

    public BookmarkData(DatenFilm film) {
        this();
        this.url = film.getUrlNormalQuality();
        this.sender = film.getSender();
        this.filmdata = film;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSender() {
        return this.sender;
    }

    public void setSender(String url) {
        this.sender = url;
    }

    public String getNote() {
        return this.note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public boolean getSeen() {
        return this.seen.get();
    }

    public boolean getNotSeen() {
        return !this.seen.get();
    }

    public void setSeen(boolean seen) {
        this.seen.set(seen);
    }

    @JsonIgnore
    public BooleanProperty getSeenProperty() {
        return seen;
    }

    @JsonIgnore
    public boolean isNotInFilmList() {
        return this.filmdata == null;
    }

    @JsonIgnore
    public boolean isLiveStream() {
        return (this.filmdata != null) ? this.filmdata.isLivestream() : false;
    }

    @JsonIgnore
    public DatenFilm getDatenFilm() {
        return this.filmdata;
    }

    @JsonIgnore
    public void setDatenFilm(DatenFilm filmdata) {
        this.filmdata = filmdata;
    }

    @JsonIgnore
    public String getWebUrl() {
        return (this.filmdata != null) ? this.filmdata.getWebsiteUrl() : null;
    }

    @JsonIgnore
    public String getFormattedNote() {
        return note != null && !note.isEmpty() ? String.format("\n\nNotiz:\n%s", note) : "";
    }

    @JsonIgnore
    public String getExtendedDescription() {
        if (filmdata == null) {
            return "Es wurde kein Filmobjekt mehr in der Filmliste gefunden. --> Ungültiger Eintrag!";
        }
        else {
            return String.format("%s - %s\n\n%s%s", sender, filmdata.getTitle(), filmdata.getDescription(), getFormattedNote());
        }
    }

    @JsonIgnore
    public Optional<DatenFilm> getDatenFilmOptional() {
        return Optional.ofNullable(filmdata);
    }
}
