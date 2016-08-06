package com.anysoftkeyboard;

import com.anysoftkeyboard.utils.Log;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import components.Distribution;
import components.Token;
import components.Touch;
import components.Window;
import trie.TrieList;

/**
 * Created by rafaniyi on 5/31/2016.
 */
public class Chain {
    private final Token.Type TOKEN_TYPE;
    private Distribution distribution;
    private List<Distribution> key_distribution;
    private volatile List<Token> tokens;
    private ArrayList<Touch> touches;
    public volatile List<Window> windows;
    public volatile List<Window> windows_s;
    public volatile List<Touch> successor_touch_s;
    public volatile List<Touch> successor_touch;
    private int window;
    private int token;
    private int threshold;
    private int model_size;
    private volatile boolean distribution_computed;
    private volatile boolean probability_computed;
    private volatile boolean key_distribution_computed;
    private volatile boolean windows_computed;
    private volatile boolean tokens_computed;

    public Chain(int window, int token, int threshold, int model_size) {
        this.TOKEN_TYPE = Token.Type.linear;
        this.key_distribution = new ArrayList<>();
        this.tokens = new ArrayList();
        this.touches = new ArrayList();
        this.windows = new TrieList();
        this.successor_touch = new ArrayList();
        this.window = window;
        this.token = token;
        this.threshold = threshold;
        this.model_size = model_size;
        this.on_model_update();
    }

    public Chain(Chain c) {
        this.TOKEN_TYPE = Token.Type.linear;
        this.key_distribution = new ArrayList(c.key_distribution);
        this.distribution = new Distribution(c.distribution);
        this.tokens = new ArrayList(c.tokens);
        this.touches = new ArrayList(c.touches);
        this.windows = new TrieList((TrieList)c.windows);
        this.windows_s = new TrieList((TrieList)c.windows_s);
        this.successor_touch = new ArrayList(c.successor_touch);
        this.successor_touch_s = new ArrayList(c.successor_touch_s);
        this.window = c.window;
        this.token = c.token;
        this.threshold = c.threshold;
        this.model_size = c.model_size;
        this.windows_computed = c.windows_computed;
        this.distribution_computed = c.distribution_computed;
        this.probability_computed = c.probability_computed;
        this.key_distribution_computed = c.key_distribution_computed;
        this.model_size = c.model_size;
        this.tokens_computed = c.tokens_computed;
    }

    public void add_touch(Touch touch) {
        this.touches.add(touch);
        Log.d("2", "touch %s", touches.get(2));
      //  Log.d("touch6", "touch %s", touches);

        if(this.touches.size() > this.model_size) {
            this.touches.remove(0);
        }

        this.on_model_update();
    }

    public void add_touch_list(List<Touch> t) {
        Iterator touch_iterator = t.iterator();

        while(touch_iterator.hasNext()) {
            this.add_touch((Touch)touch_iterator.next());
        }

    }

    public void set_distribution(Distribution distribution, List<Distribution> key_distribution) {
        this.distribution = distribution;
        this.key_distribution = key_distribution;
        this.distribution_computed = true;
        this.key_distribution_computed = true;
        this.tokens_computed = false;
        this.windows_computed = false;
        this.probability_computed = false;
    }

    public double get_touch_probability(Window w, Touch t) {
        if(!this.probability_computed) {
            this.compute_probability();
            this.probability_computed = true;
        }

        if(this.touches.size() == 0) {
            return 0.0D;
        } else if(t == null) {
            return 0.0D;
        } else {
            Touch successor = null;
            Window predecessor = null;

            for(int i = 0; i < this.successor_touch.size(); ++i) {
                if(((Touch)this.successor_touch.get(i)).compare_with_token(this.get_tokens(), t) && ((Window)this.windows.get(i)).compare_with_token(this.get_tokens(), w)) {
                    successor = (Touch)this.successor_touch.get(i);
                    predecessor = (Window)this.windows.get(i);
                    break;
                }
            }

            return successor != null && predecessor != null?successor.get_probability(predecessor):0.0D;
        }
    }

    public Distribution get_distribution() {
        if(!this.distribution_computed) {
            this.distribution = new Distribution(this.touches);
            this.distribution_computed = true;
        }

        return this.distribution;
    }

    public List<Distribution> get_key_distribution() {
        if(!this.key_distribution_computed) {
            this.compute_key_distribution();
            this.key_distribution_computed = true;
        }

        return this.key_distribution;
    }

    public int get_window() {
        return this.window;
    }

    public int get_token() {
        return this.token;
    }

    public int get_model_size() {
        return this.model_size;
    }

    public int get_threshold() {
        return this.threshold;
    }

    public void reset() {
        this.touches = new ArrayList();
        this.on_model_update();
    }

    public void compute_uncomputed() {
        this.get_distribution();
        this.get_key_distribution();
        this.get_tokens();
        this.get_windows();
        this.get_touch_probability((Window) null, (Touch) null);
    }

    public double compare_to(Chain auth_chain) {
        double difference = 0.0D;
        this.on_model_update();
        this.compute_uncomputed();
        auth_chain.set_distribution(this.get_distribution(), this.get_key_distribution());
        auth_chain.tokens_computed = false;
        auth_chain.windows_computed = false;
        auth_chain.probability_computed = false;
        auth_chain.compute_uncomputed();

        for(int i = 0; i < auth_chain.get_windows().size(); ++i) {
            difference += this.get_window_difference(this.get_windows(), this.successor_touch, (Window)auth_chain.get_windows().get(i), (Touch)auth_chain.successor_touch.get(i));
        }

        return auth_chain.get_windows().size() == 0?1.0D:difference / (double)auth_chain.get_windows().size();
    }

    private double get_window_difference(List<Window> base_window_list, List<Touch> base_successor_touch_list, Window auth_window, Touch auth_window_successor_touch) {
        double difference = 0.0D;
        int index = -1;

        for(int base_probability = 0; base_probability < base_window_list.size(); ++base_probability) {
            if(((Window)base_window_list.get(base_probability)).compare_with_token(this.get_tokens(), auth_window) && ((Touch)base_successor_touch_list.get(base_probability)).compare_with_token(this.get_tokens(), auth_window_successor_touch)) {
                index = base_probability;
                break;
            }
        }

        double var12;
        if(index == -1) {
            var12 = 0.0D;
        } else {
            var12 = ((Touch)base_successor_touch_list.get(index)).get_probability((Window)base_window_list.get(index));
        }

        double auth_probability = auth_window_successor_touch.get_probability(auth_window);
        difference = Math.abs(var12 - auth_probability);
        return difference;
    }

    private int get_base_window_index(Chain base_chain, Window auth_window, Touch auth_successor_touch, int base_start_index, int base_end_index) {
        List base_windows = base_chain.get_windows();
        List base_successor_touch = base_chain.successor_touch;

        int i;
        for(i = base_start_index; i < base_windows.size() && i < base_end_index && (!((Touch)base_successor_touch.get(i)).compare_with_token(this.tokens, auth_successor_touch) || !((Window)base_windows.get(i)).compare_with_token(this.get_tokens(), auth_window)); ++i) {
            ;
        }

        return i == base_windows.size()?-1:i;
    }

    private void on_model_update() {
        this.distribution_computed = false;
        this.probability_computed = false;
        this.key_distribution_computed = false;
        this.windows_computed = false;
        this.tokens_computed = false;
    }

    private void compute_key_distribution() {
        this.key_distribution = new ArrayList();
        ArrayList list_of_keycodes = new ArrayList();

        int i;
        for(i = 0; i < this.touches.size(); ++i) {
            int keycode_index = this.keycode_index(list_of_keycodes, ((Touch)this.touches.get(i)).get_key());
            if(keycode_index == -1) {
                ArrayList single_touch_list = new ArrayList();
                single_touch_list.add(this.touches.get(i));
                list_of_keycodes.add(single_touch_list);
            } else {
                ((List)list_of_keycodes.get(keycode_index)).add(this.touches.get(i));
            }
        }

        for(i = 0; i < list_of_keycodes.size(); ++i) {
            this.key_distribution.add(new Distribution((List)list_of_keycodes.get(i), ((Touch)((List)list_of_keycodes.get(i)).get(0)).get_key()));
        }

        this.key_distribution_computed = true;
    }

    private int keycode_index(ArrayList<List<Touch>> list_of_keycodes, int keycode) {
        int index;
        for(index = 0; index < list_of_keycodes.size() && ((Touch)((List)list_of_keycodes.get(index)).get(0)).get_key() != keycode; ++index) {
            ;
        }

        return index == list_of_keycodes.size()?-1:index;
    }

    private void compute_probability() {
        if(this.get_windows().size() != 0) {
            new ArrayList();
            ExecutorService executor = Executors.newCachedThreadPool();
            byte thread_responsibility = 100;

            for(int i = 0; i < this.windows.size(); i += thread_responsibility) {
                int end_index = i + thread_responsibility - 1;
                if(end_index >= this.windows.size()) {
                    end_index = this.windows.size() - 1;
                }

                Chain.Compute_partial_probability compute_partial = new Chain.Compute_partial_probability(i, end_index);
                Thread partial_thread = new Thread(compute_partial);
                executor.execute(partial_thread);
            }

            executor.shutdown();

            while(!executor.isTerminated()) {
                ;
            }

        }
    }

    private void compute_windows() {
        this.windows = new TrieList();
        this.successor_touch = new ArrayList();
        ArrayList touch_list = new ArrayList();
        ((TrieList)this.windows).set_tokens(this.get_tokens());



        for(int i = 0; i < this.touches.size(); ++i) {
            if(this.get_token_index((Touch)this.touches.get(i)) >= 0 && (touch_list.size() == 0 || ((Touch)this.touches.get(i)).get_timestamp() - ((Touch)touch_list.get(touch_list.size() - 1)).get_timestamp() <= (long)this.threshold) && this.is_touch_in_key_distribution((Touch)this.touches.get(i))) {
                touch_list.add(this.touches.get(i));
            } else {
                touch_list = new ArrayList();
            }

            Log.d("touch","touch1 %s",touch_list.size());

            if(touch_list.size() == this.window + 1) {
                ArrayList add_list = new ArrayList(touch_list);
                add_list.remove(add_list.size() - 1);
                this.windows.add(new Window(add_list));
                this.successor_touch.add((Touch) touch_list.get(touch_list.size() - 1));
                touch_list.remove(0);
            }
        }

    }

    public boolean is_touch_in_key_distribution(Touch touch) {
        byte sigma = 2;
        boolean is_touch_in = false;
        Distribution key_dist = null;
        List key_distributions = this.get_key_distribution();
        if(key_distributions != null) {
            for(int i = 0; i < key_distributions.size(); ++i) {
                if(((Distribution)key_distributions.get(i)).get_keycode() == touch.get_key()) {
                    key_dist = (Distribution)key_distributions.get(i);
                }
            }
        }

        if(key_dist != null && touch.get_pressure() <= key_dist.get_average() + (double)sigma * key_dist.get_standard_deviation() && touch.get_pressure() >= key_dist.get_average() - (double)sigma * key_dist.get_standard_deviation()) {
            is_touch_in = true;
        }

        return is_touch_in;
    }

    private int get_token_index(Touch touch) {
        List token_list = this.get_tokens();

        for(int i = 0; i < token_list.size(); ++i) {
            if(((Token)token_list.get(i)).contains(touch)) {
                return i;
            }
        }

        return -1;
    }

    public List<Window> get_windows() {
        if(!this.windows_computed) {
            this.compute_windows();
            this.windows_computed = true;
        }

        return this.windows;
    }

    private void compute_tokens() {
        this.tokens = new ArrayList();
        int i;
        if(this.TOKEN_TYPE == Token.Type.linear) {
            for(i = 0; i < this.token; ++i) {
                this.tokens.add(new Token(this.get_distribution(), this.token, i, 2.0D, Token.Type.linear));
            }
        } else {
            for(i = 0; i < this.get_key_distribution().size(); ++i) {
                this.tokens.add(new Token((Distribution)this.get_key_distribution().get(i), 1, i, 2.0D, Token.Type.keycode_mu));
            }
        }

    }

    public List<Token> get_tokens() {
        if(!this.tokens_computed) {
            this.compute_tokens();
            this.tokens_computed = true;
        }

        return this.tokens;
    }

    public List<Touch> get_touches() {
        return this.touches;
    }

    public String toString() {
        String s = "";
        s = s + "[";

        for(int i = 0; i < this.touches.size(); ++i) {
            s = s + ((Touch)this.touches.get(i)).toString();
            if(i < this.touches.size() - 1) {
                s = s + ", ";
            }
        }

        s = s + "]";
        return s;
    }

    public void output_to_csv(String file_name) {
        PrintWriter output = null;

        try {
            output = new PrintWriter(file_name, "UTF-8");
            output.println("[preceeding sequence] [touch pressure, probability]");

            for(int e = 0; e < this.windows.size(); ++e) {
                String predecessor_window = ((Window)this.windows.get(e)).toString();
                double touch_probability = ((Touch)this.successor_touch.get(e)).get_probability((Window)this.windows.get(e));
                double touch_pressure = ((Touch)this.successor_touch.get(e)).get_pressure();
                output.println("[" + predecessor_window + "] [" + String.format("%.4f", new Object[]{Double.valueOf(touch_pressure)}) + ", " + String.format("%.4f", new Object[]{Double.valueOf(touch_probability)}) + "]");
            }

            output.close();
        } catch (Exception var9) {
            System.out.println("Failed to open output file");
            var9.printStackTrace();
        }

    }

    public void outputProb (){


        String header = ("[preceeding sequence] [touch pressure, probability]");
        String fin = "";

        for(int e = 0; e < this.windows.size(); ++e) {
            String predecessor_window = this.windows.get(e).toString();
            double touch_probability = this.successor_touch.get(e).get_probability(this.windows.get(e));
            double touch_pressure = this.successor_touch.get(e).get_pressure();
             fin = ("[" + predecessor_window + "] [" + String.format("%.4f", new Object[]{Double.valueOf(touch_pressure)}) + ", " + String.format("%.4f", new Object[]{Double.valueOf(touch_probability)}) + "]");
             header = header.concat(fin);
           Log.d("Pressure","values  %s", header);
        }
    }

    private class Compute_partial_probability implements Runnable {
        int begin_index;
        int end_index;

        public Compute_partial_probability(int begin_index, int end_index) {
            this.begin_index = begin_index;
            this.end_index = end_index;
        }

        public void run() {
            TrieList window_list = (TrieList)Chain.this.windows;

            for(int i = this.begin_index; i <= this.end_index; ++i) {
                int occurrences_of_window = window_list.occurrence_count((Window)window_list.get(i));
                int number_successions = window_list.successor_count(Chain.this.successor_touch, (Window)window_list.get(i), (Touch)Chain.this.successor_touch.get(i));
                double probability = (double)number_successions / (double)occurrences_of_window;
                ((Touch)Chain.this.successor_touch.get(i)).set_probability((Window)window_list.get(i), probability);
            }

        }
    }
}
