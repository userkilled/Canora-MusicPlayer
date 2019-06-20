import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import android.view.View;
import android.widget.ListView;
import ch.swissproductions.canora.R;
import ch.swissproductions.canora.activities.MainActivity;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;

import static android.support.test.espresso.action.ViewActions.*;
import static android.support.test.espresso.assertion.ViewAssertions.matches;

import static android.support.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.CoreMatchers.anything;


@RunWith(AndroidJUnit4.class)
public class MainActivityEspressoTest {
    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(MainActivity.class);

    //@Test
    public void testPlayerControls() {
        for (int i = 0; i < 20; i++) {
            onView(withId(R.id.buttonPrev)).perform(click());
        }
        for (int i = 0; i < 20; i++) {
            onView(withId(R.id.buttonNex)).perform(click());
        }
    }

    @Test
    public void testSubMenus() {
        final int Intensity = 10; //Percentage of content to test
        for (int i = 0; i < 20; i++) {
            onView(withId(R.id.btnTracks)).perform(click());
            int listsize = getListItemCount(R.id.mainViewport);
            List<Integer> fields = new ArrayList<>();
            List<Integer> avail = new ArrayList<>();
            for (int y = 0; y < listsize; y++) {
                avail.add(y);
            }
            Random r = new Random();
            r.setSeed(System.currentTimeMillis());
            double iterations = Math.ceil((float) listsize / 100 * Intensity);
            for (int y = 0; y < iterations; y++) {
                int ran = r.nextInt((avail.size()) + 0);
                fields.add(new Integer(avail.get(ran)));
                avail.remove(ran);
            }
            for (int y = 0; y < fields.size(); y++) {
                onData(anything()).inAdapterView(withId(R.id.mainViewport)).atPosition(fields.get(y)).perform(click());
            }
            onView(withId(R.id.btnPlaylists)).perform(click());
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
                if (listsize > 0)
                    onData(anything()).inAdapterView(withId(R.id.mainViewport)).atPosition(fields.get(y)).perform(click());
                onView(withId(R.id.btnGenres)).perform(click());
            }
            onView(withId(R.id.SubMenuContainer)).perform(swipeRight());
        }
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
}

