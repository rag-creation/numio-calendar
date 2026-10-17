<p align="center"><img src="logo.png" width="128" alt="Calendar by Numio logo"></p>

<h1 align="center">Calendar by Numio</h1>

<p align="center">
A private, offline calendar for Android.<br>
Your events, your country's holidays, and, if you want them, the Malayalam and Hijri calendars.
</p>

<p align="center">
<img alt="License: GPL-3.0" src="https://img.shields.io/badge/license-GPL--3.0-yellow">
<img alt="Android 8.0+" src="https://img.shields.io/badge/Android-8.0%2B-yellow">
<img alt="No internet permission" src="https://img.shields.io/badge/internet-none-yellow">
</p>

---

Calendar by Numio is a calendar first: a clear month view, your events, reminders and the public
holidays of wherever you live. It works the same in Berlin, São Paulo, Dubai or Kochi.

What makes it different is what it doesn't do. It has no internet permission, so nothing you add ever
leaves your phone. No account, no ads, no tracking. Everything, from sunrise times to festival dates,
is worked out on the phone itself.

And for people who live by more than one calendar, it can show the Malayalam (Kollavarsham) and Hijri
dates under each day, with their festivals. These are optional and off with one switch; without them
it is simply a good plain calendar.

> **Status:** in testing. Test builds are on the [Releases](../../releases) page; the first stable
> release and an F-Droid listing are coming.

## For everyone

- **Month and day views.** Swipe between months and days; tap the month name to jump to any date from 1900 to 2199.
- **Events** with times or all day, a note, and a colour for personal or work.
- **Repeats:** every week, month or year.
- **Reminders** at the time, or minutes, hours, a day or a week before.
- **Public holidays for 249 countries**, 2000–2050, built into the app. Pick one or several, like your home country and the one you live in.
- **Festival countdown**: "Christmas · 69 days" chips under the month name.
- **Your phone's calendars** (Google, Samsung and others), shown read-only if you allow it.
- **Backup and restore** to a single `.numio` file that you keep wherever you like.
- **Week starts** on Sunday, Monday or Saturday.

## Regional calendars (optional)

Turn them on when the app first opens, or any time in Settings → Regional calendars.

**Malayalam calendar (Kollavarsham)**
- The Malayalam date under every day, and the day's star (nakshatram), with the time it ends.
- Find my star: from a birth date and time, the janma nakshatram, pada and kooru.
- Repeats by star, for a star birthday (pirannal) or Shraddham: "every year on Thiruvonam in Thulam".
- Repeats by Malayalam date, like every Chingam 1.
- Kerala festivals: Onam, Vishu, Thiruvathira, Makaravilakku, Sivarathri, Karkidaka Vavu,
  Vinayaka Chathurthi, Vijayadashami, Deepavali and more.

**Hijri calendar**
- The Hijri date under every day (Umm al-Qura).
- A moon-sighting adjustment of −1, 0 or +1 day, to match your country or mosque.
- Islamic festivals, marked as expected dates: Ramadan, Eid al-Fitr, Eid al-Adha, the Islamic New Year, Ashura and Milad-un-Nabi.
- Repeats by Hijri date.

More calendars are planned for this page.

## Privacy

Calendar by Numio has **no internet permission**. Android itself stops it from going online.

| Permission | Why | When it's asked |
|---|---|---|
| Read calendar | To show your phone's calendars, read-only | Only if you turn on "Show phone calendars" |
| Notifications | For reminders | The first time you set a reminder |
| Alarms, start at boot | So reminders ring on time, also after a restart | Granted at install |

Your events are kept in the app's private storage on the phone. Backups are files you save yourself.

## How the dates are worked out

There is no yearly download and no server. Everything comes from astronomy and published rules:

- **Malayalam calendar:** the positions of the Sun and Moon (formulas from Jean Meeus,
  *Astronomical Algorithms*, with the Lahiri ayanamsa) and sunrise in Kerala. A Malayalam month
  starts on the day the Sun enters a new sign before three-fifths of the daytime has passed.
  The star shown for a day is the one at sunrise.
- **Kerala festivals** follow their traditional rules: Onam is Thiruvonam in Chingam,
  Vishu is Medam 1, Deepavali is Naraka Chaturdashi in Thulam, and so on. Each rule is checked
  against published Kerala calendars for several years, and those checks run as tests on every build.
- **Hijri dates** come from the Umm al-Qura calendar built into Android. The real start of a month
  depends on the moon being sighted, so Islamic festivals are shown as expected dates.
- **Country holidays** are tables generated from the open-source
  [holidays](https://github.com/vacanza/holidays) library and shipped inside the app.

## For developers

- Kotlin and Jetpack Compose, minimum Android 8.0 (API 26), built with JDK 21.
- `./gradlew testDebugUnitTest` runs the tests, including the calendar checks against published dates.
- Every push to `main` makes a test build with GitHub Actions.
- The country holiday tables are made by `tools/make_holidays.py` (see the notes at the top of that file).

## Part of Numio

Calendar is part of [Numio](https://getnumio.org), a family of open-source Android apps:
Numio Calculator, Clock by Numio, Notes by Numio, Ezhuthola and Decibel.

## Credits

- Country public holidays are generated from the [holidays](https://github.com/vacanza/holidays)
  library by the Vacanza team and contributors, MIT licence.
- Astronomy formulas from Jean Meeus, *Astronomical Algorithms*.
- Hijri dates from the Umm al-Qura calendar in Android (ICU).

Built by RR with help from Claude.

## License

[GPL-3.0](LICENSE)
