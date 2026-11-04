# Required Android acceptance checks

Use a physical Android 15 phone and, if available, Android 16/17. Test only disposable duplicates at first.

1. Install and launch; verify branding, screen insets, navigation and Android Back behaviour.
2. Confirm live FaceTVNews headlines, images and dates. Open a story and confirm it stays inside the isolated reader. Test reader Back and Share.
3. Load more than 20 posts; check duplicates, ordering and archive pagination.
4. Select a real WordPress category and search for a published keyword. An unavailable API must show a useful error rather than stale filtered data.
5. Close/reopen the app with no internet; previously cached first-page headlines must be marked cached. Saved links do not guarantee offline full articles.
6. Add an authorised public RSS feed, then an Atom feed. Confirm source attribution, original-publisher links and source removal. Try an invalid feed and check the error.
7. Enable and deny notification permission on separate runs. Toggle alerts off and verify scheduled jobs stop. Enable again, establish a baseline, publish a new FaceTVNews post and confirm a later job alerts and opens the right article. Do not expect instant delivery.
8. Reboot and check the persisted job after the app has been enabled. Review battery restrictions if delivery is delayed.
9. Check storage/RAM figures. Clear the app cache and confirm saved links and alert settings remain. Verify no claim of guaranteed speed improvement appears.
10. Create a test folder with two identical files, one same-size different file, a nested duplicate, an empty file and a large excluded file. Select only that folder and inspect scan results.
11. Try selecting every copy from a group; deletion must be refused. Select one duplicate and cancel the native confirmation; no file should be removed.
12. Confirm deletion on a disposable duplicate. Modify the retained copy between scanning and deletion; deletion must skip the changed group.
13. Deny/revoke folder access and test an unreadable/cloud-backed file provider. Failures must not be presented as a clean/full scan.
14. Calculate a selected file's hash and compare it to a desktop SHA-256 tool. Cancel a large-file scan. No file should be uploaded.
15. Open the fingerprint lookup only after consent. Confirm that an absent report is not described as safe. Android security/settings buttons must open supported settings.
16. Before publication, supply support/privacy details, verify package identity/signing, build the API-36 release through Gradle and test that signed build. No full antivirus engine or instant-push service is included.
