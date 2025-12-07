<?php
/**
 * FASTable - Get Profile Picture API (OPTIMIZED)
 * Endpoint: https://sociallyhub.dpdns.org/fastable/get_profile_picture.php
 * Method: POST or GET
 * Parameters:
 *   - user_id: Firebase UID (required)
 *
 * Purpose: Get ONLY profile picture URL
 * User details fetched from: Local DB + Firebase (not here)
 */

include 'conn.php';
$response = array();

// Support both GET and POST
$user_id = '';
if (isset($_POST['user_id'])) {
    $user_id = mysqli_real_escape_string($conn, $_POST['user_id']);
} elseif (isset($_GET['user_id'])) {
    $user_id = mysqli_real_escape_string($conn, $_GET['user_id']);
}

if (!empty($user_id)) {
    $sql = "SELECT user_id, profile_picture_url, updated_at
            FROM user_profile_pictures
            WHERE user_id = '$user_id'";

    $result = mysqli_query($conn, $sql);

    if ($result) {
        if (mysqli_num_rows($result) > 0) {
            $row = mysqli_fetch_assoc($result);

            // Construct full URL for profile picture
            if (!empty($row['profile_picture_url'])) {
                $row['profile_picture_url'] = 'https://sociallyhub.dpdns.org/fastable/' . $row['profile_picture_url'];
            } else {
                $row['profile_picture_url'] = ''; // No profile picture
            }

            $response['status'] = 1;
            $response['message'] = "Profile picture found";
            $response['data'] = $row;
        } else {
            $response['status'] = 0;
            $response['message'] = "No profile picture found for this user";
        }
    } else {
        $response['status'] = 0;
        $response['message'] = "Database query failed: " . mysqli_error($conn);
    }
} else {
    $response['status'] = 0;
    $response['message'] = "Missing required parameter: user_id";
}

echo json_encode($response);
?>

