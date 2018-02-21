package de.digisocken.openwort;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity {
    public static final String PROJECT_LINK = "https://github.com/no-go/Wortschatz";
    public static final String FLATTR_ID = "o6wo7q";
    public static String FLATTR_LINK;
    public static final String PROJECT2_LINK = "https://www.openthesaurus.de";

    private EntryAdapter entryAdapter;
    private EntryAdapter resultEntryAdapter;
    private ListView entryList;
    private boolean copyPasteWindow;

    private ClipboardManager clipboard;
    public static SharedPreferences mPreferences;


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_copypaste:
                if (item.isChecked()) {
                    copyPasteWindow = false;
                    mPreferences.edit().putBoolean("copypaste", false).apply();
                    item.setChecked(false);
                } else {
                    copyPasteWindow = true;
                    mPreferences.edit().putBoolean("copypaste", true).apply();
                    item.setChecked(true);
                }
                break;
            case R.id.action_web:
                Intent intentProj2 = new Intent(Intent.ACTION_VIEW, Uri.parse(PROJECT2_LINK));
                startActivity(intentProj2);
                break;
            case R.id.action_flattr:
                Intent intentFlattr = new Intent(Intent.ACTION_VIEW, Uri.parse(FLATTR_LINK));
                startActivity(intentFlattr);
                break;
            case R.id.action_project:
                Intent intentProj= new Intent(Intent.ACTION_VIEW, Uri.parse(PROJECT_LINK));
                startActivity(intentProj);
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem mi2 = menu.findItem(R.id.action_copypaste);
        copyPasteWindow = mPreferences.getBoolean("copypaste", true);
        mi2.setChecked(copyPasteWindow);
        return super.onPrepareOptionsMenu(menu);
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
                resultEntryAdapter.filter(query, entryAdapter);
                entryList.setAdapter(resultEntryAdapter);
                resultEntryAdapter.notifyDataSetChanged();
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
                entryAdapter.squery = null;
                entryList.setAdapter(entryAdapter);
                entryAdapter.notifyDataSetChanged();
                return true;
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                //makeToast(getString(R.string.insertSearch));
                resultEntryAdapter.clear();
                entryList.setAdapter(resultEntryAdapter);
                resultEntryAdapter.notifyDataSetChanged();
                return true;
            }
        });

        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            FLATTR_LINK = "https://flattr.com/submit/auto?fid="+FLATTR_ID+"&url="+
                    java.net.URLEncoder.encode(PROJECT_LINK, "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        mPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        setContentView(R.layout.activity_main);
        clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        entryAdapter = new EntryAdapter(this);
        entryList = (ListView) findViewById(R.id.dicList);
        entryList.setEmptyView(findViewById(android.R.id.empty));
        entryList.setAdapter(entryAdapter);
        entryList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                DicEntry item = (DicEntry) adapterView.getItemAtPosition(i);
                String msg = item.title + "\n\n" + item.body;
                String[] buzzs = getString(R.string.buzz).split(";");
                for (int j=0; j < buzzs.length; j++) {
                    if (msg.contains(buzzs[j])) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle(R.string.titleTroll)
                                .setMessage(R.string.bodyTroll)
                                .setPositiveButton(getString(R.string.okay),
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                // TODO
                                                dialog.dismiss();
                                            }
                                        });
                        builder.show();
                        break;
                    }
                }

                if (copyPasteWindow) {
                    Intent myIntent = new Intent(MainActivity.this, EditActivity.class);
                    myIntent.putExtra("msg", msg);
                    startActivity(myIntent);
                } else {
                    ClipData clip = ClipData.newPlainText(
                            getString(R.string.app_name),
                            msg
                    );

                    makeToast(getString(R.string.copying));
                    clipboard.setPrimaryClip(clip);
                }
            }
        });

        resultEntryAdapter = new EntryAdapter(this);
        new RetrieveFeedTask().execute();
    }

    class RetrieveFeedTask extends AsyncTask<String, Void, Void> {
        protected Void doInBackground(String... dummy) {
            try {
                InputStream ins = getResources().openRawResource(R.raw.openthesaurus);
                String[] str = readTextFile(ins).split(getString(R.string.rowsplit));
                for (int i=0; i<str.length; i++) {
                    DicEntry dicEntry = new DicEntry();
                    if (str[i].trim().length() == 0) continue;
                    if (str[i].startsWith(getString(R.string.ignoreline))) continue;
                    String[] line = str[i].split(getString(R.string.columnsplit));
                    dicEntry.title = line[0];
                    str[i] = str[i].replace(line[0]+getString(R.string.columnsplit) , "");
                    dicEntry.body = str[i].replace(getString(R.string.columnsplit), getString(R.string.columnsplitReplace));
                    entryAdapter.addItem(dicEntry);
                }
                entryAdapter.sort();

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            entryAdapter.notifyDataSetChanged();
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
}
