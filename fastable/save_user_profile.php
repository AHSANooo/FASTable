<?php
/**
 * FASTable - DEPRECATED API
 *
 * This API is NO LONGER USED.
 *
 * User profile data (name, email, batch, degree, section) is stored in:
 * - Local Database (offline storage)
 * - Firebase Realtime Database (cloud sync)
 *
 * MySQL stores ONLY profile pictures.
 *
 * Use:
 * - upload_profile_picture.php (for profile pictures)
 * - get_profile_picture.php (to retrieve picture URL)
 */

header('Content-Type: application/json');
$response = array(
    'status' => 0,
    'message' => 'This API is deprecated. User details are stored in Local DB and Firebase, not MySQL. Use upload_profile_picture.php for pictures only.'
);

echo json_encode($response);
?>

