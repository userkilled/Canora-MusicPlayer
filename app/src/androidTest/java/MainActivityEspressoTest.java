import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.action.GeneralClickAction;
import android.support.test.espresso.action.GeneralLocation;
import android.support.test.espresso.action.Press;
import android.support.test.espresso.action.Tap;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiSelector;
import android.view.View;
import android.widget.ListView;
import ch.swissproductions.canora.R;
import ch.swissproductions.canora.activities.MainActivity;
import ch.swissproductions.canora.data.Constants;
import ch.swissproductions.canora.data.data_song;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.InstrumentationRegistry.getTargetContext;
import static android.support.test.espresso.Espresso.*;
import static android.support.test.espresso.action.ViewActions.*;
import static android.support.test.espresso.assertion.ViewAssertions.matches;

import static android.support.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.CoreMatchers.*;


@RunWith(AndroidJUnit4.class)
public class MainActivityEspressoTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(MainActivity.class);

    @Test
    public void GeneralHealthTest() {
        final int Intensity = 100; //Percentage of content to test
        final int Iterations = 5; //Number of Iterations
        for (int i = 0; i < Iterations; i++) {
            ExitAndResume();
            TestSettings();
            onView(withId(R.id.btnTracks)).perform(click());
            verifyListViewContent(Intensity);
            onView(withId(R.id.btnPlaylists)).perform(click());

            List<Integer> avail = new ArrayList<>();
            List<Integer> fields = new ArrayList<>();
            int listsize = getListItemCount(R.id.mainViewport);
            double iterations = Math.ceil((float) listsize / 100 * Intensity);
            Random r = new Random();
            r.setSeed(System.currentTimeMillis());

            avail.clear();
            fields.clear();
            for (int y = 0; y < listsize; y++) {
                avail.add(y);
            }
            for (int y = 0; y < iterations; y++) {
                int ran = r.nextInt((avail.size()) + 0);
                fields.add(new Integer(avail.get(ran)));
                avail.remove(ran);
            }
            for (int y = 0; y < fields.size(); y++) {
                onData(anything()).inAdapterView(withId(R.id.mainViewport)).atPosition(fields.get(y)).perform(click());
                verifyListViewContent(Intensity);
                onView(withId(R.id.btnPlaylists)).perform(click());
            }
            onView(withId(R.id.btnArtists)).perform(click());
            listsize = getListItemCount(R.id.mainViewport);
            avail.clear();
            fields.clear();
            for (int y = 0; y < listsize; y++) {
                avail.add(y);
            }
            iterations = Math.ceil((float) listsize / 100 * Intensity);
            for (int y = 0; y < iterations; y++) {
                int ran = r.nextInt((avail.size()) + 0);
                fields.add(new Integer(avail.get(ran)));
                avail.remove(ran);
            }
            for (int y = 0; y < fields.size(); y++) {
                onData(anything()).inAdapterView(withId(R.id.mainViewport)).atPosition(fields.get(y)).perform(click());
                verifyListViewContent(Intensity);
                onView(withId(R.id.btnArtists)).perform(click());
            }
            onView(withId(R.id.btnAlbums)).perform(click());
            listsize = getListItemCount(R.id.mainViewport);
            avail.clear();
            fields.clear();
            for (int y = 0; y < listsize; y++) {
                avail.add(y);
            }
            iterations = Math.ceil((float) listsize / 100 * Intensity);
            for (int y = 0; y < iterations; y++) {
                int ran = r.nextInt((avail.size()) + 0);
                fields.add(new Integer(avail.get(ran)));
                avail.remove(ran);
            }
            for (int y = 0; y < fields.size(); y++) {
                onData(anything()).inAdapterView(withId(R.id.mainViewport)).atPosition(fields.get(y)).perform(click());
                verifyListViewContent(Intensity);
                onView(withId(R.id.btnAlbums)).perform(click());
            }
            onView(withId(R.id.SubMenuContainer)).perform(swipeLeft());
            onView(withId(R.id.btnGenres)).perform(click());
            listsize = getListItemCount(R.id.mainViewport);
            avail.clear();
            fields.clear();
            for (int y = 0; y < listsize; y++) {
                avail.add(y);
            }
            iterations = Math.ceil((float) listsize / 100 * Intensity);
            for (int y = 0; y < iterations; y++) {
                int ran = r.nextInt((avail.size()) + 0);
                fields.add(new Integer(avail.get(ran)));
                avail.remove(ran);
            }
            for (int y = 0; y < fields.size(); y++) {
                onData(anything()).inAdapterView(withId(R.id.mainViewport)).atPosition(fields.get(y)).perform(click());
                verifyListViewContent(Intensity);
                onView(withId(R.id.btnGenres)).perform(click());
            }
            onView(withId(R.id.SubMenuContainer)).perform(swipeRight());
        }
    }

    private boolean verifyListViewContent(double intensity) {
        ListView ls = mActivityRule.getActivity().findViewById(R.id.mainViewport);
        View firstitem = ls.getChildAt(0);
        if (firstitem.getId() == R.id.notFound)
            return false;

        class field {
            public int pos;
            public String vtext;
        }
        List<data_song> data = mActivityRule.getActivity().vpm.getVisibleContent();
        double listsize = getListItemCount(R.id.mainViewport);
        double iterations = Math.ceil((float) listsize / 100 * intensity);
        List<field> fields = new ArrayList<>();
        List<Integer> avail = new ArrayList<>();
        for (int y = 0; y < listsize; y++) {
            avail.add(y);
        }
        Random r = new Random();
        r.setSeed(System.currentTimeMillis());
        for (int y = 0; y < iterations; y++) {
            if (avail.size() > 0) {
                int ran = r.nextInt(avail.size());
                field f = new field();
                f.pos = avail.get(ran);
                f.vtext = data.get(avail.get(ran)).Title + " by " + data.get(avail.get(ran)).Artist;
                fields.add(f);
                avail.remove(ran);
            }
        }
        for (int y = 0; y < fields.size(); y++) {
            onData(anything()).inAdapterView(withId(R.id.mainViewport)).atPosition(fields.get(y).pos).perform(click());
            onView(withId(R.id.songDisplay)).check(matches(withText(fields.get(y).vtext)));
        }
        return true;
    }

    private int getListItemCount(int id) {
        final int ret[] = new int[1];
        onView(withId(id)).check(matches(new TypeSafeMatcher<View>() {
            @Override
            public boolean matchesSafely(View view) {
                ListView listView = (ListView) view;

                ret[0] = listView.getCount();

                return true;
            }

            @Override
            public void describeTo(Description description) {

            }
        }));
        return ret[0];
    }

    private void ExitAndResume() {
        try {
            UiDevice device = UiDevice.getInstance(getInstrumentation());
            device.pressHome();
            device.pressRecentApps();
            device.findObject(new UiSelector().text(getTargetContext().getString(getTargetContext().getApplicationInfo().labelRes))).click();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void TestSettings() {
        final int eqsize = 3; //Number of Equalizer Spinner Items
        Random r = new Random();
        r.setSeed(System.currentTimeMillis());
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getTargetContext());
        onView(withText(mActivityRule.getActivity().getString(R.string.menu_options_settings))).perform(click());
        int t = r.nextInt(3);
        switch (t) {
            case 0:
                onView(withId(R.id.seekBar1)).perform(new GeneralClickAction(Tap.SINGLE, GeneralLocation.CENTER_LEFT, Press.FINGER));//Volume
                break;
            case 1:
                onView(withId(R.id.seekBar1)).perform(new GeneralClickAction(Tap.SINGLE, GeneralLocation.CENTER_RIGHT, Press.FINGER));//Volume
                break;
            case 2:
                onView(withId(R.id.seekBar1)).perform(new GeneralClickAction(Tap.SINGLE, GeneralLocation.CENTER, Press.FINGER));//Volume
                break;
        }
        onView(withId(R.id.spinner)).perform(click());
        onData(allOf(is(instanceOf(String.class)))).atPosition(r.nextInt(Constants.Themes.theThemes.get().size())).perform(click());
        onView(withId(R.id.spinnerEqualizer)).perform(click());
        onData(allOf(is(instanceOf(String.class)))).atPosition(r.nextInt(eqsize)).perform(click());
        UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        mDevice.pressBack();
    }
}

