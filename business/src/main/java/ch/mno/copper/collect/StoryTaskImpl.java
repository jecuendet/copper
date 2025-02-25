package ch.mno.copper.collect;

import ch.mno.copper.stories.data.Story;
import it.sauronsoftware.cron4j.Predictor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Usage:
 * call shouldRun, then getRunnable, then markAsRun.
 * Created by dutoitc on 02.02.2016.
 */
public class StoryTaskImpl implements StoryTask {
    private Logger LOG = LoggerFactory.getLogger(getClass());

    private Runnable runnable;
    private long lastRun=-1;
    private static long nextId=1;
    private long taskId = nextId++;
    private Story story;
    private boolean running = false;
    private CronData cronData = null;

    private class CronData {
        private long nextRun;
        private String cronExpression;

        public CronData(String cronExpression) {
            this.cronExpression = cronExpression;
            computeNextRun();
        }

        private void computeNextRun() {
            if (cronExpression==null) {
                LOG.error("Null Cron Expression for story " + story.getName());
            }
            Predictor p = new Predictor(cronExpression);
            nextRun = p.nextMatchingTime();
            LOG.info("Task {}: scheduled next run in {}", taskId + (story==null?"":("[" + story.getName() + "]")), computeTime(nextRun-System.currentTimeMillis()));
        }

        public boolean shouldRun() {
            return System.currentTimeMillis()>=nextRun;
        }
    }

    /**
     * pattern minutes(0-59) hours(0-23) day_of_month(1-31), month(1-12 or jan,feb...), days of week(0-6=sunday-saturday or sun mon tue...) e.g. 0 3 * jan-jun,sep-dec mon-fri
     *                @see {http://www.sauronsoftware.it/projects/cron4j/manual.php}
     * @param runnable
     * @param cronExpression
     */
    public StoryTaskImpl(Story story, Runnable runnable, String cronExpression) {
        this.runnable = runnable;
        this.story = story;
        this.cronData = new CronData(cronExpression);
    }

    @Override
    public String storyName() {
        return story.getName();
    }

    @Override
    public String getTitle() {
        return story.getName().replace(".txt", "");
    }



    @Override
    public long getTaskId() {
        return taskId;
    }

    @Override
    public Runnable getRunnable() {
        return runnable;
    }

    @Override
    public boolean shouldRun() {
        if (running) return false;    // Running flag avoid double execution for tasks scheduled every minute, if task run take more than one minute.

        if (cronData!=null) {
            return cronData.shouldRun();
        }

        return false;
    }

    @Override
    public long getNextRun() {
        if (cronData!=null) {
            return cronData.nextRun;
        }
        return -1;
    }

    @Override
    public void markAsRunning() {
        running = true;
    }

    @Override
    public void markAsRun() {
        lastRun = System.currentTimeMillis();
        if (cronData!=null) cronData.computeNextRun();
        running = false;
    }

    private String computeTime(long millis) {
        if (millis<=0) return "(now)";

        StringBuilder sb = new StringBuilder();
        if (millis >= 1000*3600*24) {
            int days = (int)(millis/1000/3600/24);
            sb.append(days).append(" days");
            millis-=days*1000*3600*24;
        }
        if (millis>=1000*3600) {
            int hours = (int)(millis/1000/3600);
            if (sb.length()>0) sb.append(", ");
            sb.append(hours).append(" hours");
            millis-=hours*1000*3600;
        }
        if (millis>=1000*60) {
            int min = (int)(millis/1000/60);
            if (sb.length()>0) sb.append(", ");
            sb.append(min).append(" minutes");
            millis-=min*1000*3600;
        }
        if (millis>=1000) {
            int sec = (int)(millis/1000);
            if (sb.length()>0) sb.append(", ");
            sb.append(sec).append(" seconds");
            millis-=sec*1000;
        }
        return sb.toString();
    }

}
