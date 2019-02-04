package oss.dndcomplaint;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.fragment.app.ListFragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

public class SmsListFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int REQUEST_CODE_SMS_PERMISSION = 1337;
    private static final long MILLIS_IN_A_DAY = 24*60*60*1000;
    private static final SimpleDateFormat sdf =
            new SimpleDateFormat("dd-MMM hh:mm a", Locale.getDefault());

    private static String[] smsDataProjection = {
            Telephony.Sms.ADDRESS,
            Telephony.Sms.DATE,
            Telephony.Sms.BODY,
            Telephony.Sms._ID
    };

    SimpleCursorAdapter adapter;

    private void init() {
        setEmptyText("SMS inbox empty");
        sdf.setTimeZone(TimeZone.getDefault());

//        setHasOptionsMenu(true);
        adapter = new SimpleCursorAdapter(getActivity(), R.layout.sms, null, smsDataProjection,
                new int[] {R.id.sender, R.id.date, R.id.body}, 0);


        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                TextView tv = (TextView) view;
                if (columnIndex != 1) {
                    tv.setText(cursor.getString(columnIndex));
                } else {
                    tv.setText(SmsListFragment.sdf.format(SmsListFragment.this.extractDate(cursor)));
                }
                return true;
            }
        });

        setListAdapter(adapter);
        LoaderManager.getInstance(this).initLoader(0, null, this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            case REQUEST_CODE_SMS_PERMISSION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    init();
                } else {
                    Toast.makeText(getContext(), "SMS permission required", Toast.LENGTH_LONG).show();
                }
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.READ_SMS},
                    REQUEST_CODE_SMS_PERMISSION);
        }
        else {
            init();
        }
    }

//    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        // Place an action bar item for searching.
//        MenuItem item = menu.add("Refresh");
//        item.setIcon(android.R.drawable.ic_menu_revert);
//        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
//    }

    private Date extractDate(Cursor c) {
        long milliTimeStamp = Long.parseLong(c.getString(1));
        return new Date(milliTimeStamp);
    }

    @Override public void onListItemClick(ListView l, View v, int position, long id) {
        Cursor c = (Cursor)adapter.getItem(position);

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));

        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"1909@airtel.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Unsolicited SMS complaint");

        String emailBody = "This is a complaint for receiving unsolicited sms on my number " +
                "\n\nDetails:\n\n" +
                "Sender:\n" +
                c.getString(0) +
                "\n\nDate & Time: " +
                sdf.format(extractDate(c)) +
                "\n\nMessageBody:\n" +
                c.getString(2);
        intent.putExtra(Intent.EXTRA_TEXT, emailBody);
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(getActivity(), "No email app found!", Toast.LENGTH_LONG).show();
        }
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        long cutoff = System.currentTimeMillis() - 3*MILLIS_IN_A_DAY;
        return new CursorLoader(getActivity(),
                Telephony.Sms.CONTENT_URI,
                smsDataProjection,
                "date > ?",
                new String[]{Long.toString(cutoff)},
                Telephony.Sms.DEFAULT_SORT_ORDER);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }
}