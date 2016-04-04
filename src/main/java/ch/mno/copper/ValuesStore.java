package ch.mno.copper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * A Store for values. Timestamp is set as data insertion.
 * Put will erase data. get will return null or -1 if not found.
 * Created by dutoitc on 29.01.2016.
 */
public class ValuesStore {

    // TODO: dependency graph, with temporized triggers and quiet time

    private static final Logger LOG = LoggerFactory.getLogger(ValuesStore.class);
    private static final ValuesStore instance = new ValuesStore();
    private Map<String, StoreValue> map = new HashMap<>();
    private Set<String> changedValues = new HashSet<>();

    static {
        try {
            instance.load(new FileInputStream("valuesStore.tmp"));
        } catch (IOException e) {
            System.err.println("Cannot load valuesStore.tmp, ignoring");
        }
    }

    public static ValuesStore getInstance() {
        return instance;
    }

    public void put(String key, String value) {
        if (map.containsKey(key) && map.get(key).value.equals(value)) return;
        map.put(key, new StoreValue(value));
        changedValues.add(key);
    }

    public Map<String, StoreValue> getValues() {
        return map;
    }

    public void save(OutputStream os) throws IOException {
        PrintWriter pw = new PrintWriter(os);
        pw.write("1\n");
        pw.write(map.size()+"\n");
        map.forEach((k,v)->pw.write(k+"|"+v.getValue().replace("|", "£").replace("\n","¢")+"|"+v.getTimestamp()+"\n"));
        pw.write(""+changedValues.size()+'\n');
        changedValues.forEach(k->pw.write(k+'\n'));
        pw.flush();
        pw.close();
        os.flush();
    }

    public void load(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        // Version
        int version = Integer.parseInt(reader.readLine());
        if (version!=1) throw new RuntimeException("Unsupported version: " + version);

        // Size
        int mapSize = Integer.parseInt(reader.readLine());
        map = new HashMap<>(mapSize*4/3);

        // Map
        for (int noLine=0; noLine<mapSize; noLine++) {
            try {
                String[] values = reader.readLine().split("\\|");
                map.put(values[0], new StoreValue(values[1].replaceAll("£", "|").replace("¢", "\n"), Long.parseLong(values[2])));
            } catch (Exception e) {
                throw new RuntimeException("Error at line #" + noLine + ": " + e.getMessage(), e);
            }
        }

        // List
        int listSize = Integer.parseInt(reader.readLine());
        changedValues = new HashSet<String>(listSize*4/3);
        for (int i=0; i<listSize; i++) {
            changedValues.add(reader.readLine());
        }
        System.out.println("ValuesStore loaded from tmp file");
    }

    public Collection<String> getChangedValues() {
        // TODO: Improve this with a quiet time for stable values ?
        List<String> values = new ArrayList<>(changedValues);
        changedValues.clear();
        return values;
    }

    public String getValue(String key) {
        if (map.containsKey(key)) {
            return map.get(key).value;
        }
        if (key.startsWith("NOW_")) {
            // NOW_DD.MM.YY_HH:MM
            String pattern = key.substring(4).replace("_", " ");
            try {
                return new SimpleDateFormat(pattern).format(new Date());
            } catch (IllegalArgumentException e) {
                LOG.error("Unparseable pattern: " + pattern + "; " + e.getMessage());
            }
        }

        return null;
    }

    public long getTimestamp(String key) {
        if (map.containsKey(key)) {
            return map.get(key).timestamp;
        }
        return -1;
    }

    /**
     *
     * @param desc key1,key2...
     * @param values values for keys, ordered
     */
    public void putAll(String desc, List<String> values) {
        String[] keys = desc.split(",");
        if (keys.length!=values.size()) throw new RuntimeException("Wrong number of parameters");
        for (int i=0; i<keys.length; i++) {
            put(keys[i],values.get(i));
        }
    }

    public Map<String, String> getValuesMapString() {
        // FIXME make it Java8
        Map<String, String> values = new HashMap<>();
        map.forEach((k,v)->values.put(k,v.getValue()));
        return values;
    }

    public void clear() {
        map.clear();
        changedValues.clear();
    }

    public static class StoreValue {
        private String value;
        private long timestamp;
        public StoreValue(String value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }

        StoreValue(String value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "StoreValue{" +
                    "value='" + value + '\'' +
                    ", timestamp=" + timestamp +
                    '}';
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

}
