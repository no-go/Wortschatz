package de.quantenwatch.wortschatz;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity {
    public static final String PROJECT_LINK = "https://www.openthesaurus.de";

    private FeedAdapter feedAdapter;
    private FeedAdapter resultFeedAdapter;
    private ListView feedList;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_web) {
            Intent intentProj = new Intent(Intent.ACTION_VIEW, Uri.parse(PROJECT_LINK));
            startActivity(intentProj);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actionbar_menu, menu);
        super.onCreateOptionsMenu(menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchView searchView =
                (SearchView) MenuItemCompat.getActionView(searchItem);

        searchView.setOnQueryTextListener(
        new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                makeToast(getString(R.string.searching, query));
                resultFeedAdapter.filter(query, feedAdapter);
                feedList.setAdapter(resultFeedAdapter);
                resultFeedAdapter.notifyDataSetChanged();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) { return false;}
        });

        MenuItemCompat.setOnActionExpandListener(
        searchItem,
        new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                makeToast(getString(R.string.leaveSearch));
                feedAdapter.squery = null;
                feedList.setAdapter(feedAdapter);
                feedAdapter.notifyDataSetChanged();
                return true;
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                makeToast(getString(R.string.insertSearch));
                resultFeedAdapter.clear();
                feedList.setAdapter(resultFeedAdapter);
                resultFeedAdapter.notifyDataSetChanged();
                return true;
            }
        });

        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        feedAdapter = new FeedAdapter(this);
        feedList = (ListView) findViewById(R.id.feedList);
        feedList.setEmptyView(findViewById(android.R.id.empty));
        feedList.setAdapter(feedAdapter);

        resultFeedAdapter = new FeedAdapter(this);
        new RetrieveFeedTask().execute();
    }

    class RetrieveFeedTask extends AsyncTask<String, Void, Void> {
        protected Void doInBackground(String... dummy) {
            try {
                InputStream ins = getResources().openRawResource(R.raw.openthesaurus);
                String[] str = readTextFile(ins).split(getString(R.string.rowsplit));
                for (int i=0; i<str.length; i++) {
                    Feed feed = new Feed();
                    if (str[i].trim().length() == 0) continue;
                    if (str[i].startsWith(getString(R.string.ignoreline))) continue;
                    String[] line = str[i].split(getString(R.string.columnsplit));
                    feed.title = line[0];
                    feed.body = str[i].replace(getString(R.string.columnsplit), getString(R.string.columnsplitReplace));
                    feedAdapter.addItem(feed);
                }
                sortFeedArrayList();

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            feedAdapter.notifyDataSetChanged();
        }
    }

    public void makeToast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    public String readTextFile(InputStream inputStream) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, len);
            }
            outputStream.close();
            inputStream.close();
        } catch (IOException e) {}
        return outputStream.toString();
    }

    public void sortFeedArrayList() {
        final String[] from = getString(R.string.sortReplaceFrom).split(getString(R.string.columnsplit));
        final String[] to = getString(R.string.sortReplaceTo).split(getString(R.string.columnsplit));

        Collections.sort(feedAdapter.feeds, new Comparator<Feed>() {
            @Override
            public int compare(Feed f1, Feed f2) {
                String s1 = f1.title;
                String s2 = f2.title;
                for (int i =0; i < from.length; i++) {
                    s1 = s1.replace(from[i], to[i]);
                    s2 = s2.replace(from[i], to[i]);
                }
                return s1.compareTo(s2);
            }
        });
    }
}
