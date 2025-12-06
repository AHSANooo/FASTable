<?php
// upload-profile-image.php
// Place this file in your Apache server's htdocs/api/ folder

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $json = file_get_contents('php://input');
    $data = json_decode($json, true);

    if (isset($data['userId']) && isset($data['image'])) {
        $userId = $data['userId'];
        $base64Image = $data['image'];

        // Remove base64 header if present
        $base64Image = preg_replace('/^data:image\/\w+;base64,/', '', $base64Image');

        // Decode base64
        $imageData = base64_decode($base64Image);

        // Create uploads directory if it doesn't exist
        $uploadDir = '../uploads/profile_images/';
        if (!file_exists($uploadDir)) {
            mkdir($uploadDir, 0777, true);
        }

        // Generate filename
        $filename = $userId . '_' . time() . '.jpg';
        $filepath = $uploadDir . $filename;

        // Save image
        if (file_put_contents($filepath, $imageData)) {
            // Return image URL
            // Replace YOUR_LAPTOP_IP with your actual IP address
            $imageUrl = 'http://YOUR_LAPTOP_IP:8080/uploads/profile_images/' . $filename;

            echo json_encode([
                'success' => true,
                'imageUrl' => $imageUrl,
                'message' => 'Image uploaded successfully'
            ]);
        } else {
            http_response_code(500);
            echo json_encode([
                'success' => false,
                'message' => 'Failed to save image'
            ]);
        }
    } else {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'Missing userId or image data'
        ]);
    }
} else {
    http_response_code(405);
    echo json_encode([
        'success' => false,
        'message' => 'Method not allowed'
    ]);
}
?>

