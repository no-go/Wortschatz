package de.quantenwatch.wortschatz;

import android.app.Activity;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class FeedAdapter extends BaseAdapter {
    public String squery;
    public ArrayList<Feed> feeds = new ArrayList<>();
    Activity activity;

    FeedAdapter(Activity context) {
        super();
        squery = null;
        activity = context;
    }

    public void addItem(Feed item) {
        feeds.add(item);
    }

    public void clear() {
        feeds.clear();
    }

    public void filter(String query, FeedAdapter feedAdapter) {
        clear();
        squery = query;
        query = query.toLowerCase();
        for (int i = 0; i < feedAdapter.getCount(); i++) {
            Feed feed = (Feed) feedAdapter.getItem(i);
            if (feed.title.toLowerCase().contains(query) || feed.body.toLowerCase().contains(query)) {
                addItem(feed);
            }
        }
    }

    @Override
    public int getCount() {
        return feeds.size();
    }

    @Override
    public Object getItem(int i) {
        return feeds.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        view = activity.getLayoutInflater().inflate(R.layout.feed_line, viewGroup, false);
        TextView tt = (TextView) view.findViewById(R.id.line_title);
        TextView tb = (TextView) view.findViewById(R.id.line_body);
        if (squery != null) {
            tt.setText(highlight(squery, feeds.get(i).title));
            tb.setText(highlight(squery, feeds.get(i).body));
        } else {
            tt.setText(feeds.get(i).title);
            tb.setText(feeds.get(i).body);
        }
        return view;
    }

    public Spanned highlight(String key, String msg) {
        msg = msg.replaceAll(
                "((?i)"+key+")",
                "<b><font color='"
                        + ContextCompat.getColor(activity.getApplicationContext(),
                        R.color.colorAccent) +
                        "'>$1</font></b>"
        );
        return fromHtml(msg);
    }

    static public Spanned fromHtml(String str) {
        Spanned sp;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            sp = Html.fromHtml(str, Html.FROM_HTML_MODE_COMPACT, null, null);
        } else {
            sp = Html.fromHtml(str, null, null);
        }
        return sp;
    }
}