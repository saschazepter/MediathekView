package mediathek.gui.tabs.tab_film.helpers;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import mediathek.gui.tabs.tab_film.SearchControlFieldMode;
import mediathek.gui.tabs.tab_film.SearchFieldData;
import mediathek.gui.tabs.tab_film.filter.FilmFilterController;
import mediathek.tool.FilterConfiguration;
import mediathek.tool.FilterDTO;
import org.apache.commons.configuration2.XMLConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

class GuiModelHelperSupportTest {

    @Test
    void createFilterExecutionContext_usesLiveControllerStateWhenFilterIsLocked() throws Exception {
        var filterConfiguration = new TestFilterConfiguration(new XMLConfiguration());
        var filter = new FilterDTO(UUID.randomUUID(), "Filter 1");
        filterConfiguration.addNewFilter(filter);
        filterConfiguration.setCurrentFilter(filter);

        var controller = new FilmFilterController(
                filterConfiguration,
                new FilmFilterController.DataProvider() {
                    @Override
                    public EventList<String> senderList() {
                        return new BasicEventList<>();
                    }

                    @Override
                    public List<String> getThemen(Collection<String> senders) {
                        return Collections.emptyList();
                    }
                },
                new FilmFilterController.ReloadRequester() {
                    @Override
                    public void requestTableReload() {
                    }

                    @Override
                    public void requestZeitraumReload() {
                    }
                });

        controller.setCurrentFilterChangesLocked(true);
        controller.onSenderSelectionChanged(Collections.singleton("ARD"));

        var support = new GuiModelHelperSupport(
                new SearchFieldData("", SearchControlFieldMode.THEMA_TITEL),
                controller);

        var context = support.createFilterExecutionContext();

        Assertions.assertEquals(Collections.singleton("ARD"), context.selectedSenders());
        Assertions.assertTrue(filterConfiguration.getCheckedChannels().isEmpty());
    }

    private static final class TestFilterConfiguration extends FilterConfiguration {
        private TestFilterConfiguration(XMLConfiguration configuration) {
            super(configuration);
        }
    }
}
