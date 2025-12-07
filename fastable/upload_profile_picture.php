<?php
/**
 * FASTable - Upload/Update Profile Picture API (OPTIMIZED)
 * Endpoint: https://sociallyhub.dpdns.org/fastable/upload_profile_picture.php
 * Method: POST
 * Parameters:
 *   - user_id: Firebase UID (required)
 *   - image: Base64 encoded image string (required)
 *
 * Purpose: Store ONLY profile pictures
 * User details stored in: Local DB + Firebase (not here)
 */

include 'conn.php';
$response = array();

if (isset($_POST['image'], $_POST['user_id'])) {
    $user_id = mysqli_real_escape_string($conn, $_POST['user_id']);
    $image = $_POST['image']; // Base64 encoded string

    // Generate unique filename (using .jpg since we're uploading JPEG format)
    $imageName = 'profile_' . $user_id . '_' . time() . '.jpg';
    $uploadPath = 'uploads/' . $imageName;

    // Create uploads directory if it doesn't exist
    if (!file_exists('uploads')) {
        mkdir('uploads', 0777, true);
    }

    // Remove base64 prefix if present
    if (strpos($image, 'data:image') === 0) {
        $image = explode(',', $image)[1];
    }

    // Decode base64 string
    $decodedImage = base64_decode($image);

    // Check if decoding succeeded
    if ($decodedImage !== false) {
        // Save image to server
        if (file_put_contents($uploadPath, $decodedImage)) {
            // Delete old profile picture if exists
            $checkSql = "SELECT profile_picture_url FROM user_profile_pictures WHERE user_id = '$user_id'";
            $result = mysqli_query($conn, $checkSql);

            if ($result && mysqli_num_rows($result) > 0) {
                $row = mysqli_fetch_assoc($result);
                $oldPicture = $row['profile_picture_url'];

                // Delete old file if it exists
                if (!empty($oldPicture) && file_exists($oldPicture)) {
                    @unlink($oldPicture);
                }

                // Update existing record
                $updateSql = "UPDATE user_profile_pictures
                             SET profile_picture_url = '$uploadPath',
                                 updated_at = CURRENT_TIMESTAMP
                             WHERE user_id = '$user_id'";

                if (mysqli_query($conn, $updateSql)) {
                    $response['status'] = 1;
                    $response['message'] = "Profile picture updated successfully";
                    $response['image_url'] = 'https://sociallyhub.dpdns.org/fastable/' . $uploadPath;
                } else {
                    $response['status'] = 0;
                    $response['message'] = "Database update failed: " . mysqli_error($conn);
                }
            } else {
                // New user - insert record (ONLY picture, no user details)
                $insertSql = "INSERT INTO user_profile_pictures (user_id, profile_picture_url)
                             VALUES ('$user_id', '$uploadPath')";

                if (mysqli_query($conn, $insertSql)) {
                    $response['status'] = 1;
                    $response['message'] = "Profile picture uploaded successfully";
                    $response['image_url'] = 'https://sociallyhub.dpdns.org/fastable/' . $uploadPath;
                } else {
                    $response['status'] = 0;
                    $response['message'] = "Database insert failed: " . mysqli_error($conn);
                }
            }
        } else {
            $response['status'] = 0;
            $response['message'] = "Failed to save image file";
        }
    } else {
        $response['status'] = 0;
        $response['message'] = "Base64 decode failed";
    }
} else {
    $response['status'] = 0;
    $response['message'] = "Missing required parameters: user_id and image";
}

echo json_encode($response);
?>

