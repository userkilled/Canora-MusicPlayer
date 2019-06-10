package ch.swissproductions.canora.managers;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import ch.swissproductions.canora.R;
import ch.swissproductions.canora.activities.MainActivity;
import ch.swissproductions.canora.data.Constants;
import ch.swissproductions.canora.data.data_song;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class ViewPortManager {
    /* Manipulates the ListView and the new Control Widgets */

    public int state = Constants.ARRAYADAPT_STATE_DEFAULT;

    public int subMenu;

    public ViewPortManager(MainActivity maPar, ListView viewPortPar, View controlsPar, DataManager plp) {
        viewPort = viewPortPar;
        controlButtons = controlsPar;
        dm = plp;
        ma = maPar;
        songAdaptContentRef = new ArrayList<>();
        songAdapt = new SongAdapter(ma, songAdaptContentRef);
        stringAdaptContentRef = new ArrayList<>();
        stringAdapt = new StringAdapter(ma, stringAdaptContentRef);
    }

    public void showCustom(List<data_song> inp) {
        final List<data_song> input = inp;
        subMenu = Constants.DATA_SELECTOR_NONE;
        ma.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                songAdaptContentRef.clear();
                songAdaptContentRef.addAll(input);
                viewPort.setAdapter(songAdapt);
                songAdapt.notifyDataSetChanged();
                if (input.size() == 0) {
                    viewPort.setOnItemClickListener(null);
                    return;
                }
                viewPort.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Log.v(LOG_TAG, "POSITION: " + position + " ID " + dm.dataout.get(position).id + " NAME " + dm.dataout.get(position).Title);
                        if (!dm.filesfound)
                            return;
                        ImageButton btn = ma.findViewById(R.id.buttonPlay);
                        if (ma.serv != null) {
                            if (ma.serv.play(dm.dataout.get(position).id) == 0)
                                ma.setPlayButton(btn, true);
                            else
                                ma.setPlayButton(btn, false);
                            ma.serv.setEqualizerPreset(Integer.parseInt(ma.sc.getSetting(Constants.SETTING_EQUALIZERPRESET)));
                            ma.updateSongDisplay();
                            ma.handleProgressAnimation(ma.serv.getDuration(), ma.serv.getCurrentPosition());
                        }
                    }
                });
                viewPort.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            }
        });
    }

    public void showTracks() {
        subMenu = Constants.DATA_SELECTOR_NONE;
        ma.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                colorSelectedMenu(0);
                dm.selectTracks();
                songAdaptContentRef.clear();
                Log.v(LOG_TAG, "DATAOUTSIZE: " + dm.dataout.size());
                songAdaptContentRef.addAll(dm.dataout);
                ma.serv.setContent(dm.dataout);
                viewPort.setAdapter(songAdapt);
                songAdapt.notifyDataSetChanged();
                if (dm.dataout.size() == 0) {
                    viewPort.setOnItemClickListener(null);
                    return;
                }
                viewPort.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Log.v(LOG_TAG, "POSITION: " + position + " ID " + dm.dataout.get(position).id + " NAME " + dm.dataout.get(position).Title);
                        if (!dm.filesfound)
                            return;
                        ImageButton btn = ma.findViewById(R.id.buttonPlay);
                        if (ma.serv != null) {
                            if (ma.serv.play(dm.dataout.get(position).id) == 0)
                                ma.setPlayButton(btn, true);
                            else
                                ma.setPlayButton(btn, false);
                            ma.serv.setEqualizerPreset(Integer.parseInt(ma.sc.getSetting(Constants.SETTING_EQUALIZERPRESET)));
                            ma.updateSongDisplay();
                            ma.handleProgressAnimation(ma.serv.getDuration(), ma.serv.getCurrentPosition());
                        }
                    }
                });
                viewPort.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            }
        });
    }

    public void showPlaylists() {
        subMenu = Constants.DATA_SELECTOR_PLAYLISTS;
        ma.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                colorSelectedMenu(1);
                stringAdaptContentRef.clear();
                stringAdaptContentRef.addAll(dm.getPlaylists());
                viewPort.setAdapter(stringAdapt);
                stringAdapt.notifyDataSetChanged();
                if (dm.getPlaylists().size() == 0) {
                    viewPort.setOnItemClickListener(null);
                    return;
                }
                viewPort.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        subMenu = Constants.DATA_SELECTOR_NONE;
                        dm.selectPlayList(dm.getPlaylists().get(position));
                        songAdaptContentRef.clear();
                        songAdaptContentRef.addAll(dm.dataout);
                        ma.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                viewPort.setAdapter(songAdapt);
                                songAdapt.notifyDataSetChanged();
                                viewPort.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                        if (!dm.filesfound)
                                            return;
                                        ImageButton btn = ma.findViewById(R.id.buttonPlay);
                                        if (ma.serv != null) {
                                            if (ma.serv.play(dm.dataout.get(position).id) == 0)
                                                ma.setPlayButton(btn, true);
                                            else
                                                ma.setPlayButton(btn, false);
                                            ma.serv.setEqualizerPreset(Integer.parseInt(ma.sc.getSetting(Constants.SETTING_EQUALIZERPRESET)));
                                            ma.updateSongDisplay();
                                            ma.handleProgressAnimation(ma.serv.getDuration(), ma.serv.getCurrentPosition());
                                        }
                                    }
                                });
                                viewPort.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                                ma.notifyAAandOM();
                            }
                        });
                        ma.serv.setContent(dm.dataout);
                    }
                });
            }
        });
    }

    public void showPlaylist(String name) {
        colorSelectedMenu(1);
        subMenu = Constants.DATA_SELECTOR_NONE;
        dm.selectPlayList(name);
        songAdaptContentRef.clear();
        songAdaptContentRef.addAll(dm.dataout);
        ma.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewPort.setAdapter(songAdapt);
                songAdapt.notifyDataSetChanged();
                viewPort.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        if (!dm.filesfound)
                            return;
                        ImageButton btn = ma.findViewById(R.id.buttonPlay);
                        if (ma.serv != null) {
                            if (ma.serv.play(dm.dataout.get(position).id) == 0)
                                ma.setPlayButton(btn, true);
                            else
                                ma.setPlayButton(btn, false);
                            ma.serv.setEqualizerPreset(Integer.parseInt(ma.sc.getSetting(Constants.SETTING_EQUALIZERPRESET)));
                            ma.updateSongDisplay();
                            ma.handleProgressAnimation(ma.serv.getDuration(), ma.serv.getCurrentPosition());
                        }
                    }
                });
                viewPort.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                ma.notifyAAandOM();
            }
        });
        ma.serv.setContent(dm.dataout);
    }

    public void showArtists() {
        subMenu = Constants.DATA_SELECTOR_STATICPLAYLISTS_ARTISTS;
        ma.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stringAdaptContentRef.clear();
                stringAdaptContentRef.addAll(dm.getArtists());
                colorSelectedMenu(2);
                viewPort.setAdapter(stringAdapt);
                stringAdapt.notifyDataSetChanged();
                if (dm.getArtists().size() == 0) {
                    viewPort.setOnItemClickListener(null);
                    return;
                }
                viewPort.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        subMenu = Constants.DATA_SELECTOR_NONE;
                        dm.selectArtist(dm.getArtists().get(position));
                        songAdaptContentRef.clear();
                        songAdaptContentRef.addAll(dm.dataout);
                        ma.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                viewPort.setAdapter(songAdapt);
                                songAdapt.notifyDataSetChanged();
                                viewPort.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                        if (!dm.filesfound)
                                            return;
                                        ImageButton btn = ma.findViewById(R.id.buttonPlay);
                                        if (ma.serv != null) {
                                            if (ma.serv.play(dm.dataout.get(position).id) == 0)
                                                ma.setPlayButton(btn, true);
                                            else
                                                ma.setPlayButton(btn, false);
                                            ma.serv.setEqualizerPreset(Integer.parseInt(ma.sc.getSetting(Constants.SETTING_EQUALIZERPRESET)));
                                            ma.updateSongDisplay();
                                            ma.handleProgressAnimation(ma.serv.getDuration(), ma.serv.getCurrentPosition());
                                        }
                                    }
                                });
                                ma.notifyAAandOM();
                            }
                        });
                        viewPort.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                        ma.serv.setContent(dm.dataout);
                    }
                });
            }
        });
    }

    public void showArtist(String name) {
        colorSelectedMenu(2);
        subMenu = Constants.DATA_SELECTOR_NONE;
        dm.selectArtist(name);
        songAdaptContentRef.clear();
        songAdaptContentRef.addAll(dm.dataout);
        ma.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewPort.setAdapter(songAdapt);
                songAdapt.notifyDataSetChanged();
                viewPort.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        if (!dm.filesfound)
                            return;
                        ImageButton btn = ma.findViewById(R.id.buttonPlay);
                        if (ma.serv != null) {
                            if (ma.serv.play(dm.dataout.get(position).id) == 0)
                                ma.setPlayButton(btn, true);
                            else
                                ma.setPlayButton(btn, false);
                            ma.serv.setEqualizerPreset(Integer.parseInt(ma.sc.getSetting(Constants.SETTING_EQUALIZERPRESET)));
                            ma.updateSongDisplay();
                            ma.handleProgressAnimation(ma.serv.getDuration(), ma.serv.getCurrentPosition());
                        }
                    }
                });
                viewPort.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                ma.notifyAAandOM();
            }
        });
        ma.serv.setContent(dm.dataout);
    }

    public void showAlbums() {
        subMenu = Constants.DATA_SELECTOR_STATICPLAYLISTS_ALBUMS;
        ma.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stringAdaptContentRef.clear();
                stringAdaptContentRef.addAll(dm.getAlbums());
                viewPort.setAdapter(stringAdapt);
                stringAdapt.notifyDataSetChanged();
                colorSelectedMenu(3);
                if (dm.getAlbums().size() == 0) {
                    viewPort.setOnItemClickListener(null);
                    return;
                }
                viewPort.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        subMenu = Constants.DATA_SELECTOR_NONE;
                        dm.selectAlbum(dm.getAlbums().get(position));
                        songAdaptContentRef.clear();
                        songAdaptContentRef.addAll(dm.dataout);
                        ma.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                viewPort.setAdapter(songAdapt);
                                songAdapt.notifyDataSetChanged();
                                viewPort.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                        if (!dm.filesfound)
                                            return;
                                        ImageButton btn = ma.findViewById(R.id.buttonPlay);
                                        if (ma.serv != null) {
                                            if (ma.serv.play(dm.dataout.get(position).id) == 0)
                                                ma.setPlayButton(btn, true);
                                            else
                                                ma.setPlayButton(btn, false);
                                            ma.serv.setEqualizerPreset(Integer.parseInt(ma.sc.getSetting(Constants.SETTING_EQUALIZERPRESET)));
                                            ma.updateSongDisplay();
                                            ma.handleProgressAnimation(ma.serv.getDuration(), ma.serv.getCurrentPosition());
                                        }
                                    }
                                });
                                viewPort.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                                ma.notifyAAandOM();
                            }
                        });
                        ma.serv.setContent(dm.dataout);
                    }
                });
            }
        });
    }

    public void showAlbum(String album) {
        colorSelectedMenu(3);
        subMenu = Constants.DATA_SELECTOR_NONE;
        dm.selectAlbum(album);
        songAdaptContentRef.clear();
        songAdaptContentRef.addAll(dm.dataout);
        ma.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewPort.setAdapter(songAdapt);
                songAdapt.notifyDataSetChanged();
                viewPort.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        if (!dm.filesfound)
                            return;
                        ImageButton btn = ma.findViewById(R.id.buttonPlay);
                        if (ma.serv != null) {
                            if (ma.serv.play(dm.dataout.get(position).id) == 0)
                                ma.setPlayButton(btn, true);
                            else
                                ma.setPlayButton(btn, false);
                            ma.serv.setEqualizerPreset(Integer.parseInt(ma.sc.getSetting(Constants.SETTING_EQUALIZERPRESET)));
                            ma.updateSongDisplay();
                            ma.handleProgressAnimation(ma.serv.getDuration(), ma.serv.getCurrentPosition());
                        }
                    }
                });
                viewPort.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                ma.notifyAAandOM();
            }
        });
        ma.serv.setContent(dm.dataout);
    }

    public void showGenres() {
        subMenu = Constants.DATA_SELECTOR_STATICPLAYLISTS_GENRES;
        ma.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stringAdaptContentRef.clear();
                stringAdaptContentRef.addAll(dm.getGenres());
                viewPort.setAdapter(stringAdapt);
                colorSelectedMenu(4);
                stringAdapt.notifyDataSetChanged();
                if (dm.getGenres().size() == 0) {
                    viewPort.setOnItemClickListener(null);
                    return;
                }
                viewPort.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        subMenu = Constants.DATA_SELECTOR_NONE;
                        dm.selectGenre(dm.getGenres().get(position));
                        songAdaptContentRef.clear();
                        songAdaptContentRef.addAll(dm.dataout);
                        ma.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                viewPort.setAdapter(songAdapt);
                                songAdapt.notifyDataSetChanged();
                                viewPort.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                        if (!dm.filesfound)
                                            return;
                                        ImageButton btn = ma.findViewById(R.id.buttonPlay);
                                        if (ma.serv != null) {
                                            if (ma.serv.play(dm.dataout.get(position).id) == 0)
                                                ma.setPlayButton(btn, true);
                                            else
                                                ma.setPlayButton(btn, false);
                                            ma.serv.setEqualizerPreset(Integer.parseInt(ma.sc.getSetting(Constants.SETTING_EQUALIZERPRESET)));
                                            ma.updateSongDisplay();
                                            ma.handleProgressAnimation(ma.serv.getDuration(), ma.serv.getCurrentPosition());
                                        }
                                    }
                                });
                                viewPort.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                                ma.notifyAAandOM();
                            }
                        });
                        ma.serv.setContent(dm.dataout);
                    }
                });
            }
        });
    }

    public void showGenre(String name) {
        colorSelectedMenu(4);
        subMenu = Constants.DATA_SELECTOR_NONE;
        dm.selectGenre(name);
        songAdaptContentRef.clear();
        songAdaptContentRef.addAll(dm.dataout);
        ma.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewPort.setAdapter(songAdapt);
                songAdapt.notifyDataSetChanged();
                viewPort.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        if (!dm.filesfound)
                            return;
                        ImageButton btn = ma.findViewById(R.id.buttonPlay);
                        if (ma.serv != null) {
                            if (ma.serv.play(dm.dataout.get(position).id) == 0)
                                ma.setPlayButton(btn, true);
                            else
                                ma.setPlayButton(btn, false);
                            ma.serv.setEqualizerPreset(Integer.parseInt(ma.sc.getSetting(Constants.SETTING_EQUALIZERPRESET)));
                            ma.updateSongDisplay();
                            ma.handleProgressAnimation(ma.serv.getDuration(), ma.serv.getCurrentPosition());
                        }
                    }
                });
                viewPort.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                ma.notifyAAandOM();
            }
        });
        ma.serv.setContent(dm.dataout);
    }

    public void reload() {
        switch (dm.getSelector()) {
            case Constants.DATA_SELECTOR_PLAYLISTS:
                showPlaylist(dm.getIndex());
                break;
            case Constants.DATA_SELECTOR_STATICPLAYLISTS_ALBUMS:
                showAlbum(dm.getIndex());
                break;
            case Constants.DATA_SELECTOR_STATICPLAYLISTS_ARTISTS:
                showArtist(dm.getIndex());
                break;
            case Constants.DATA_SELECTOR_STATICPLAYLISTS_GENRES:
                showGenre(dm.getIndex());
                break;
            case Constants.DATA_SELECTOR_STATICPLAYLISTS_TRACKS:
                showTracks();
                break;
        }
    }

    public String searchTerm = "";
    public int searchBy;

    public void showFiltered(String term, int srb) {
        Log.v(LOG_TAG, "SHOWFILTERED");
        if (subMenu != Constants.DATA_SELECTOR_NONE)
            return;
        try {
            searchTerm = term;
            searchBy = srb;
            if (!dm.filesfound)
                return;
            List<data_song> cl = new ArrayList<>(dm.dataout);
            Log.v(LOG_TAG, "CONTENT SIZE:" + cl.size());
            List<data_song> flt = new ArrayList<>();
            switch (srb) {
                case Constants.SEARCH_BYTITLE:
                    Log.v(LOG_TAG, "SEARCH BY TITLE");
                    for (int i = 0; i < cl.size(); i++) {
                        if (compareStrings(cl.get(i).Title, term)) {
                            flt.add(cl.get(i));
                        }
                    }
                    break;
                case Constants.SEARCH_BYARTIST:
                    Log.v(LOG_TAG, "SEARCH BY ARTIST");
                    for (int i = 0; i < cl.size(); i++) {
                        if (compareStrings(cl.get(i).Artist, term)) {
                            flt.add(cl.get(i));
                        }
                    }
                    break;
                case Constants.SEARCH_BYBOTH:
                    Log.v(LOG_TAG, "SEARCH BY BOTH");
                    for (int i = 0; i < cl.size(); i++) {
                        if (compareStrings(cl.get(i).Title, term) || compareStrings(cl.get(i).Artist, term)) {
                            flt.add(cl.get(i));
                        }
                    }
                    break;
            }
            final List<data_song> inp = flt;
            ma.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showCustom(inp);
                    ma.notifyAAandOM();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void notifyAA() {
        ma.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (songAdapt != null)
                    songAdapt.notifyDataSetChanged();
                if (stringAdapt != null)
                    stringAdapt.notifyDataSetChanged();
            }
        });
    }

    public void setClicked(int pos) {
        songAdapt.setClicked(pos);
    }

    public int getClicked() {
        return songAdapt.clicked;
    }

    public void deleteSelectedPlaylists() {
        List<String> del = stringAdapt.getSelected();
        for (int i = 0; i < del.size(); i++) {
            dm.deletePlayList(del.get(i));
            for (int y = 0; y < stringAdaptContentRef.size(); y++) {
                if (stringAdaptContentRef.get(y) == del.get(i)) {
                    stringAdaptContentRef.remove(y);
                }
            }
        }
        ma.notifyAAandOM();
    }

    public List<String> getSelected() {
        return stringAdapt.getSelected();
    }

    private ListView viewPort;
    private View controlButtons;
    private DataManager dm;

    private SongAdapter songAdapt;
    private List<data_song> songAdaptContentRef;
    private StringAdapter stringAdapt;
    private List<String> stringAdaptContentRef;

    private MainActivity ma;

    private String LOG_TAG = "VPM";

    private static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException
                    (l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }

    private static int safeDoubleToInt(double l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException
                    (l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }

    private String leftpadZero(int val) {
        if ((val - 10) < 0) {
            String rt = "0" + val;
            return rt;
        } else {
            String rt = "" + val;
            return rt;
        }
    }

    private boolean compareStrings(String haystack, String needle) {
        return Pattern.compile(Pattern.quote(needle), Pattern.CASE_INSENSITIVE).matcher(haystack).find();
    }

    private static void expandTouchArea(final View bigView, final View smallView, final int extraPadding) {
        bigView.post(new Runnable() {
            @Override
            public void run() {
                Rect rect = new Rect();
                smallView.getHitRect(rect);
                rect.top -= extraPadding;
                rect.left -= extraPadding;
                rect.right += extraPadding;
                rect.bottom += extraPadding;
                bigView.setTouchDelegate(new TouchDelegate(rect, smallView));
            }
        });
    }

    private void colorSelectedMenu(int sel) {
        LinearLayout root = (LinearLayout) controlButtons;
        Log.v(LOG_TAG, "COLOR: " + sel + " CHILD COUNT: " + root.getChildCount());
        //Colors the Active SubMenu Button on Top of the Listview
        for (int i = 0; i < root.getChildCount(); i++) {
            RelativeLayout child = (RelativeLayout) root.getChildAt(i);
            ImageButton imb = (ImageButton) child.getChildAt(0);
            TextView tmb = (TextView) child.getChildAt(1);
            if (i == sel) {
                Log.v(LOG_TAG, "HIGHLIGHTING " + i);
                imb.getDrawable().mutate().setColorFilter(ma.getColorFromAtt(R.attr.colorHighlight), PorterDuff.Mode.SRC_ATOP);
                tmb.setTextColor(ma.getColorFromAtt(R.attr.colorHighlight));
            } else {
                Log.v(LOG_TAG, "NORMAL " + i);
                imb.getDrawable().mutate().setColorFilter(ma.getColorFromAtt(R.attr.colorText), PorterDuff.Mode.SRC_ATOP);
                tmb.setTextColor(ma.getColorFromAtt(R.attr.colorText));
            }
        }
    }

    //Arrayadapter of Submenu List
    private class StringAdapter extends ArrayAdapter<String> {
        private List<String> pls;
        private List<Boolean> sel;
        public boolean empty = false;

        public StringAdapter(@NonNull Context context, List<String> data) {
            super(context, 0, data);
            pls = data;
            sel = new ArrayList<>();
        }

        @Override
        public void notifyDataSetChanged() {
            sel.clear();
            for (int i = 0; i < pls.size(); i++) {
                sel.add(false);
            }
            if (pls.size() == 0) {
                Log.v(LOG_TAG, "EMPTY");
                empty = true;
                pls.add(null);
            } else if (pls.get(0) != null) {
                Log.v(LOG_TAG, "NOT EMPTY");
                empty = false;
            }
            super.notifyDataSetChanged();
        }

        @NonNull
        @Override
        public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View listItem = convertView;
            if (empty) {
                listItem = LayoutInflater.from(ma).inflate(R.layout.list_item_notfound, parent, false);
                TextView tv = listItem.findViewById(R.id.centerText);
                switch (subMenu) {
                    case Constants.DATA_SELECTOR_PLAYLISTS:
                        tv.setText(R.string.misc_noplaylistfound);
                        break;
                    case Constants.DATA_SELECTOR_STATICPLAYLISTS_ALBUMS:
                        tv.setText(R.string.misc_noAlbumfound);
                        break;
                    case Constants.DATA_SELECTOR_STATICPLAYLISTS_ARTISTS:
                        tv.setText(R.string.misc_noArtistfound);
                        break;
                    case Constants.DATA_SELECTOR_STATICPLAYLISTS_GENRES:
                        tv.setText(R.string.misc_nogenresfound);
                        break;
                    case Constants.DATA_SELECTOR_STATICPLAYLISTS_TRACKS:
                        tv.setText(R.string.misc_notracksfound);
                        break;
                    default:
                        break;
                }
                return listItem;
            }
            if (listItem == null || listItem.findViewById(R.id.subMenuTitle) == null)
                listItem = LayoutInflater.from(ma).inflate(R.layout.list_item_submenu, parent, false);
            TextView plt = listItem.findViewById(R.id.subMenuTitle);
            ImageView im = listItem.findViewById(R.id.arrow);
            plt.setText(pls.get(position));
            if (dm.getSelector() == subMenu) {
                if (dm.getIndex().equals(pls.get(position))) {
                    plt.setTextColor(ma.getColorFromAtt(R.attr.colorHighlight));
                    im.getDrawable().setColorFilter(ma.getColorFromAtt(R.attr.colorHighlight), PorterDuff.Mode.MULTIPLY);
                } else {
                    plt.setTextColor(ma.getColorFromAtt(R.attr.colorText));
                    im.getDrawable().setColorFilter(ma.getColorFromAtt(R.attr.colorText), PorterDuff.Mode.MULTIPLY);
                }
            } else {
                plt.setTextColor(ma.getColorFromAtt(R.attr.colorText));
                im.getDrawable().setColorFilter(ma.getColorFromAtt(R.attr.colorText), PorterDuff.Mode.MULTIPLY);
            }
            final CheckBox mcb = listItem.findViewById(R.id.checkbox);
            final View exp = listItem.findViewById(R.id.hitbox);
            switch (state) {
                case Constants.ARRAYADAPT_STATE_SELECT:
                    if (subMenu != Constants.DATA_SELECTOR_PLAYLISTS)
                        break;
                    mcb.setVisibility(View.VISIBLE);
                    if (sel.size() >= position - 1 && sel.size() != 0)
                        mcb.setChecked(sel.get(position));
                    else
                        mcb.setChecked(false);
                    mcb.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            if (((CheckBox) v).isChecked()) {
                                sel.set(position, true);

                            } else {
                                sel.set(position, false);
                            }
                        }
                    });
                    expandTouchArea(exp, (View) mcb, 1000);
                    break;
                default:
                    mcb.setVisibility(View.GONE);
                    expandTouchArea(exp, (View) mcb, 0);
                    break;
            }
            return listItem;
        }

        public List<String> getSelected() {
            List<String> ret = new ArrayList<>();
            for (int i = 0; i < sel.size(); i++) {
                if (sel.get(i)) {
                    ret.add(pls.get(i));
                }
            }
            return ret;
        }
    }

    //ArrayAdapter of Song List Display
    private class SongAdapter extends ArrayAdapter<data_song> {
        private Context mContext;
        private List<data_song> viewList;

        private boolean empty;

        public SongAdapter(@NonNull Context context, List<data_song> list) {
            super(context, 0, list);
            mContext = context;
            viewList = list;
            empty = false;
        }

        @Override
        public void notifyDataSetChanged() {
            if (viewList.size() == 0) {
                Log.v(LOG_TAG, "EMPTY");
                empty = true;
                viewList.add(new data_song());
            } else if (viewList.get(0).Title != null) {
                Log.v(LOG_TAG, "NOT EMPTY");
                empty = false;
            }
            super.notifyDataSetChanged();
        }

        @NonNull
        @Override
        public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View listItem = convertView;
            if (empty) {
                listItem = LayoutInflater.from(mContext).inflate(R.layout.list_item_notfound, parent, false);
                TextView tx = listItem.findViewById(R.id.centerText);
                tx.setText(R.string.misc_notracksfound);
                return listItem;
            }
            if (listItem == null || listItem.findViewById(R.id.listsongname) == null)
                listItem = LayoutInflater.from(mContext).inflate(R.layout.list_item_song, parent, false);
            if (viewList.get(position) == null)
                return listItem;
            TextView sn = listItem.findViewById(R.id.listsongname);
            TextView in = listItem.findViewById(R.id.listinterpret);
            TextView ln = listItem.findViewById(R.id.songlength);
            sn.setText(viewList.get(position).Title);
            in.setText(viewList.get(position).Artist);
            String lstr = "" + leftpadZero(safeLongToInt(TimeUnit.MILLISECONDS.toMinutes(viewList.get(position).length))) + ":" + leftpadZero(safeLongToInt(TimeUnit.MILLISECONDS.toSeconds(viewList.get(position).length - TimeUnit.MILLISECONDS.toMinutes(viewList.get(position).length) * 60000)));
            ln.setText(lstr);
            final CheckBox mcb = listItem.findViewById(R.id.checkbox);
            final View exp = listItem.findViewById(R.id.hitbox);
            switch (state) {
                case Constants.ARRAYADAPT_STATE_SELECT:
                    mcb.setVisibility(View.VISIBLE);
                    mcb.setChecked(viewList.get(position).selected);
                    mcb.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            if (((CheckBox) v).isChecked()) {
                                viewList.get(position).selected = true;

                            } else {
                                viewList.get(position).selected = false;
                            }
                        }
                    });
                    expandTouchArea(exp, (View) mcb, 1000);
                    break;
                default:
                    mcb.setVisibility(View.GONE);
                    expandTouchArea(exp, (View) mcb, 0);
                    break;
            }
            return listItem;
        }

        public int clicked;

        public void setClicked(int pos) {
            clicked = pos;
        }
    }
}
