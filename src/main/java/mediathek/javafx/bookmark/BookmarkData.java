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
 * @author K. Wich
 * 
 * Note: Prepared for Jackson JSON storage
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookmarkData {
  private String url;
  private String sender;
  @JsonProperty("seen")
  private final BooleanProperty seen;
  @JsonIgnore
  private DatenFilm filmdata;
  private String highQualityUrl;
  private String urlKlein;
  private String note;

  public BookmarkData() {
    seen = new SimpleBooleanProperty(false);
  }
 
  public BookmarkData(DatenFilm film) {
    this();
    this.url = film.getUrlNormalQuality();
    this.sender = film.getSender();
    this.highQualityUrl = film.getHighQualityUrl();
    this.urlKlein = film.getLowQualityUrl();
    this.filmdata = film;
  }

  // getter/setter used for Jackson
  public String getUrl(){ return this.url; }
  public void   setUrl(String url){ this.url = url; }

  public String getSender(){ return this.sender; }
  public void   setSender(String url){ this.sender = url; }

  public String getNote(){ return this.note; }
  public void   setNote(String note){ this.note = note; }
  
  public boolean getSeen(){ return this.seen.get(); }
  public void   setSeen(boolean seen){ this.seen.set(seen);}

  public String getHighQualityUrl(){ return this.highQualityUrl; }
  public void   setHighQualityUrl(String highQualityUrl){ this.highQualityUrl = highQualityUrl;}
  
  public String getUrlKlein() { return urlKlein; }
  public void setUrlKlein(String urlKlein) { this.urlKlein = urlKlein; }
  
  // property access:
  @JsonIgnore
  public BooleanProperty getSeenProperty() { return seen; }
  
  // other methods:
  @JsonIgnore
  public boolean hasURL() {
    return this.url != null;
  }
  
  @JsonIgnore
  public boolean hasWebURL() {
    return (this.filmdata != null && !this.filmdata.getWebsiteUrl().isEmpty());
  }

  /**
   * Compare with URL and Sender to get unique movie
   * @param url String
   * @param sender String
   * @return true if equal
   */
  @JsonIgnore
  public boolean isMovie(String url, String sender) {
    return this.url.compareTo(url) == 0 && this.sender.compareTo(sender) == 0;
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
  public void setDatenFilm(DatenFilm filmdata) {
    this.filmdata = filmdata;
  }
  
  @JsonIgnore
  public DatenFilm getDatenFilm() {
    return this.filmdata;
  }
  
  @JsonIgnore
  public String getWebUrl() {
    return (this.filmdata != null) ? this.filmdata.getWebsiteUrl() : null;
  }

  @JsonIgnore
  public String getFormattedNote() {
    return note != null && !note.isEmpty() ? String.format("\n\nAnmerkung:\n%s", note) : "";
  }
  
  @JsonIgnore
  public String getExtendedDescription() {
    var description = (filmdata != null) ? filmdata.getDescription() : "";
    String titel = (filmdata != null) ? filmdata.getTitle() : "";
    return String.format("%s - %s\n\n%s%s", sender, titel, description, getFormattedNote());
  }

  @JsonIgnore
  public Optional<DatenFilm> getDatenFilmOptional() {
    return Optional.ofNullable(filmdata);
  }
}
