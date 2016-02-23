package ch.mno.copper;

import ch.mno.copper.stories.Story;

import java.util.List;

/**
 * Created by dutoitc on 14.02.2016.
 */
public class CopperMediator {


    private static final CopperMediator instance = new CopperMediator();
    private List<Story> stories;
    private CopperDaemon daemon;

    public static CopperMediator getInstance() { return instance; }


    public void setStories(List<Story> stories) {
        this.stories = stories;
    }

    public List<Story> getStories() {
        return stories;
    }

    public void run(String storyName) {
        daemon.runStory(storyName);
    }

    public void registerCopperDaemon(CopperDaemon daemon) {
        this.daemon = daemon;
    }
}
