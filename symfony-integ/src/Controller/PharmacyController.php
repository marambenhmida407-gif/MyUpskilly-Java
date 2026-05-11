<?php
namespace App\Controller;

use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

#[Route('/pharmacies')]
#[IsGranted('ROLE_USER')]
class PharmacyController extends AbstractController
{
    private const OVERPASS_SERVERS = [
        'https://overpass-api.de/api/interpreter',
        'https://overpass.kumi.systems/api/interpreter',
        'https://maps.mail.ru/osm/tools/overpass/api/interpreter',
    ];

    #[Route('', name: 'app_pharmacies')]
    public function index(): Response
    {
        return $this->render('pharmacy/map.html.twig');
    }

    // Proxy Overpass — évite les restrictions CORS du navigateur
    #[Route('/api/search', name: 'app_pharmacies_search', methods: ['GET'])]
    public function searchPharmacies(Request $req): JsonResponse
    {
        $lat = (float) $req->query->get('lat', 36.8065);
        $lng = (float) $req->query->get('lng', 10.1815);

        $query = "[out:json][timeout:25];"
            . "("
            . "node[\"amenity\"=\"pharmacy\"](around:5000,$lat,$lng);"
            . "way[\"amenity\"=\"pharmacy\"](around:5000,$lat,$lng);"
            . ");out center;";

        $encoded = urlencode($query);

        foreach (self::OVERPASS_SERVERS as $server) {
            $ctx = stream_context_create([
                'http' => [
                    'timeout' => 20,
                    'header'  => "User-Agent: EspritMedical/1.0\r\n"
                               . "Accept: application/json\r\n",
                ],
            ]);
            $response = @file_get_contents("$server?data=$encoded", false, $ctx);
            if ($response && str_contains($response, '"elements"')) {
                return new JsonResponse(json_decode($response, true));
            }
        }

        return new JsonResponse(['elements' => [], 'error' => 'Serveur indisponible'], 503);
    }

    // Geocoding ville → coordonnées via Nominatim
    #[Route('/api/geocode', name: 'app_pharmacies_geocode', methods: ['GET'])]
    public function geocode(Request $req): JsonResponse
    {
        $city = $req->query->get('city', '');
        if (empty($city)) {
            return new JsonResponse(['error' => 'Ville manquante'], 400);
        }

        $encoded  = urlencode($city . ', Tunisia');
        $url      = "https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=1";
        $ctx = stream_context_create([
            'http' => [
                'timeout' => 10,
                'header'  => "User-Agent: EspritMedical/1.0 (student project)\r\n",
            ],
        ]);
        $response = @file_get_contents($url, false, $ctx);
        if (!$response) {
            return new JsonResponse(['error' => 'Geocoding indisponible'], 503);
        }

        $data = json_decode($response, true);
        if (empty($data)) {
            return new JsonResponse(['error' => 'Ville introuvable'], 404);
        }

        return new JsonResponse([
            'lat' => (float) $data[0]['lat'],
            'lng' => (float) $data[0]['lon'],
        ]);
    }
}