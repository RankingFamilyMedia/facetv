# FaceTVNews community publishing

Version 1.0.4 adds a small app entry point, while WordPress remains the source of truth for public content.

## WordPress setup

1. Copy `C:\Users\PCs\Documents\facetvnews\facetvnews\chromenews\page-contribute.php` into the active ChromeNews theme directory.
2. Apply the matching footer change from `C:\Users\PCs\Documents\facetvnews\facetvnews\chromenews\footer.php`, or add a menu item pointing to the new page.
3. In WordPress, create a page titled **Contribute** with slug `contribute` and choose the **FaceTV Community Submission** template.
4. Decide whether to enable **Anyone can register**. If enabled, use the lowest default role and keep `New User Default Role` set to Subscriber. Editors still approve every submission.
5. Create or test a student account and an employee account. Open `https://facetvnews.com/contribute/`, sign in, submit a headline, story and optional image, then confirm the post appears under **Posts → Pending**.
6. Approve the post in WordPress. The existing REST feed used by the Android app will include the post after publication, including its featured image.

## Safety rules

- The form accepts authenticated WordPress users only.
- Every submission is forced to `pending`; contributors cannot publish directly.
- WordPress nonces, escaped output, `wp_kses_post`, title limits and WordPress media validation are used.
- Do not put WordPress administrator credentials, application passwords or REST write tokens in the APK.
- Review copyright, privacy, defamation and personal-data issues before approval.

## App behavior

The **You → Write and submit** button opens the WordPress page inside the protected in-app reader. This keeps the APK small and lets WordPress handle login cookies, account recovery and the editorial form. No second Firebase content database is required.

The Android client is ready for the page, but the website template must be deployed and the WordPress page created before the button can accept submissions in production.

