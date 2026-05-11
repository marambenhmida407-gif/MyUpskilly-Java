<?php
namespace App\Controller;

use App\Entity\Patologie;
use App\Repository\PatologieRepository;
use App\Repository\UserRepository;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

use PhpOffice\PhpSpreadsheet\Spreadsheet;
use PhpOffice\PhpSpreadsheet\Writer\Xlsx;
use PhpOffice\PhpSpreadsheet\Style\Fill;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\StreamedResponse;

#[Route('/patologies')]
#[IsGranted('ROLE_DOCTOR')]
class PatologieController extends AbstractController
{
    #[Route('', name: 'app_patologies')]
    public function index(PatologieRepository $repo, Request $req): Response
    {
        $all     = $repo->findAllWithUser();
        $keyword = $req->query->get('search', '');
        $type    = $req->query->get('type', '');
        $gravite = $req->query->get('gravite', '');
        if ($keyword || $type || $gravite) {
            $all = array_filter($all, function($p) use ($keyword, $type, $gravite) {
                return (!$keyword || stripos($p->getNom(), $keyword) !== false)
                    && (!$type    || $p->getType() === $type)
                    && (!$gravite || $p->getGravite() === $gravite);
            });
        }
        $stats = ['total' => count($all), 'faible' => 0, 'moderee' => 0, 'critique' => 0];
        foreach ($all as $p) { if (isset($stats[$p->getGravite()])) $stats[$p->getGravite()]++; }
        return $this->render('patologie/index.html.twig', ['patologies' => $all, 'stats' => $stats, 'keyword' => $keyword, 'type' => $type, 'gravite' => $gravite]);
    }

    #[Route('/new', name: 'app_patologie_new', methods: ['GET', 'POST'])]
    public function new(Request $req, PatologieRepository $repo, UserRepository $userRepo): Response
    {
        if ($req->isMethod('POST')) {
            $p = new Patologie();
            $p->setUserId((int)$req->request->get('user_id'))->setNom($req->request->get('nom'))->setDescription($req->request->get('description') ?: null)->setType($req->request->get('type'))->setGravite($req->request->get('gravite'));
            $repo->savePatologie($p);
            $this->addFlash('success', 'Pathologie ajoutée !');
            return $this->redirectToRoute('app_patologies');
        }
        return $this->render('patologie/form.html.twig', ['patologie' => null, 'patients' => $userRepo->findAllPatients(), 'types' => ['chronique', 'infectieuse', 'hereditaire', 'degenerative', 'aigue'], 'gravites' => ['faible', 'moderee', 'critique']]);
    }

    #[Route('/{id}/edit', name: 'app_patologie_edit', methods: ['GET', 'POST'])]
    public function edit(int $id, Request $req, PatologieRepository $repo, UserRepository $userRepo): Response
    {
        $all = $repo->findAllWithUser();
        $pat = null;
        foreach ($all as $p) { $ref = new \ReflectionClass($p); $ip = $ref->getProperty('id'); $ip->setAccessible(true); if ($ip->getValue($p) === $id) { $pat = $p; break; } }
        if (!$pat) throw $this->createNotFoundException();
        if ($req->isMethod('POST')) {
            $pat->setUserId((int)$req->request->get('user_id'))->setNom($req->request->get('nom'))->setDescription($req->request->get('description') ?: null)->setType($req->request->get('type'))->setGravite($req->request->get('gravite'));
            $repo->updatePatologie($pat);
            $this->addFlash('success', 'Pathologie modifiée !');
            return $this->redirectToRoute('app_patologies');
        }
        return $this->render('patologie/form.html.twig', ['patologie' => $pat, 'patients' => $userRepo->findAllPatients(), 'types' => ['chronique', 'infectieuse', 'hereditaire', 'degenerative', 'aigue'], 'gravites' => ['faible', 'moderee', 'critique']]);
    }

    #[Route('/{id}/delete', name: 'app_patologie_delete', methods: ['POST'])]
    public function delete(int $id, PatologieRepository $repo): Response
    { $repo->deleteById($id); $this->addFlash('success', 'Supprimée.'); return $this->redirectToRoute('app_patologies'); }


    #[Route('/export-excel', name: 'app_patologie_export', methods: ['GET'])]
public function exportExcel(PatologieRepository $repo): StreamedResponse
{
    $all = $repo->findAllWithUser();

    $spreadsheet = new Spreadsheet();
    $sheet = $spreadsheet->getActiveSheet();
    $sheet->setTitle('Pathologies');

    $headers = ['#', 'Nom', 'Type', 'Gravité', 'Description', 'Patient'];
    $cols    = ['A','B','C','D','E','F'];

    foreach ($headers as $i => $h) {
        $sheet->setCellValue($cols[$i].'1', $h);
        $sheet->getStyle($cols[$i].'1')->applyFromArray([
            'font' => ['bold' => true, 'color' => ['rgb' => 'FFFFFF']],
            'fill' => ['fillType' => Fill::FILL_SOLID, 'startColor' => ['rgb' => '0A5F7A']],
        ]);
    }

    foreach ($all as $i => $p) {
        $row = $i + 2;
        $ref = new \ReflectionClass($p);
        $idP = $ref->getProperty('id'); $idP->setAccessible(true);
        $sheet->setCellValue("A$row", $idP->getValue($p));
        $sheet->setCellValue("B$row", $p->getNom());
        $sheet->setCellValue("C$row", $p->getType() ?? '');
        $sheet->setCellValue("D$row", $p->getGravite() ?? '');
        $sheet->setCellValue("E$row", $p->getDescription() ?? '');
        $sheet->setCellValue("F$row", $p->getUserNom() ?? '');

        $color = match($p->getGravite()) {
            'faible'   => 'D1FAE5',
            'moderee'  => 'FEF3C7',
            'critique' => 'FEE2E2',
            default    => 'FFFFFF',
        };
        $sheet->getStyle("D$row")->getFill()
            ->setFillType(Fill::FILL_SOLID)
            ->getStartColor()->setRGB($color);
    }

    foreach ($cols as $col) {
        $sheet->getColumnDimension($col)->setAutoSize(true);
    }

    return new StreamedResponse(function () use ($spreadsheet) {
        (new Xlsx($spreadsheet))->save('php://output');
    }, 200, [
        'Content-Type'        => 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        'Content-Disposition' => 'attachment; filename="pathologies_' . date('Y-m-d') . '.xlsx"',
        'Cache-Control'       => 'max-age=0',
    ]);
}

#[Route('/fda-search', name: 'app_patologie_fda', methods: ['GET'])]
public function fdaSearch(Request $req): JsonResponse
{
    $nom = trim($req->query->get('nom', ''));
    if (empty($nom)) return new JsonResponse(['results' => []]);

    // Dictionnaire fr → en (même que Java)
    $traductions = [
        'diabète' => 'diabetes', 'diabete' => 'diabetes',
        'hypertension' => 'hypertension', 'cancer' => 'cancer',
        'grippe' => 'influenza', 'asthme' => 'asthma',
        'allergie' => 'allergy', 'infection' => 'infection',
        'douleur' => 'pain', 'fièvre' => 'fever', 'fievre' => 'fever',
        'migraine' => 'migraine', 'arthrite' => 'arthritis',
        'dépression' => 'depression', 'depression' => 'depression',
        'anxiété' => 'anxiety', 'anxiete' => 'anxiety',
        'insomnie' => 'insomnia', 'tuberculose' => 'tuberculosis',
        'hépatite' => 'hepatitis', 'hepatite' => 'hepatitis',
        'cholestérol' => 'cholesterol', 'cholesterol' => 'cholesterol',
        'bronchite' => 'bronchitis', 'pneumonie' => 'pneumonia',
        'gastrite' => 'gastritis', 'eczéma' => 'eczema',
        'psoriasis' => 'psoriasis', 'sinusite' => 'sinusitis',
        'anémie' => 'anemia', 'anemie' => 'anemia',
    ];

    $searchTerm = $traductions[strtolower($nom)] ?? $nom;
    $encoded    = urlencode($searchTerm);
    $url        = "https://api.fda.gov/drug/label.json?search=indications_and_usage:{$encoded}&limit=8";

    $ctx      = stream_context_create(['http' => ['timeout' => 10, 'header' => "User-Agent: EspritMedical/1.0\r\n"]]);
    $response = @file_get_contents($url, false, $ctx);

    if (!$response) {
        // Fallback: chercher dans brand_name
        $url2     = "https://api.fda.gov/drug/label.json?search=openfda.brand_name:{$encoded}&limit=8";
        $response = @file_get_contents($url2, false, $ctx);
    }

    if (!$response) return new JsonResponse(['results' => [], 'searchTerm' => $searchTerm]);

    $data    = json_decode($response, true);
    $results = [];

    foreach ($data['results'] ?? [] as $drug) {
        $brandNames = $drug['openfda']['brand_name'] ?? [];
        $genericNames = $drug['openfda']['generic_name'] ?? [];
        $name = $brandNames[0] ?? $genericNames[0] ?? 'Médicament inconnu';

        $indication = '';
        if (!empty($drug['indications_and_usage'])) {
            $indication = substr(strip_tags($drug['indications_and_usage'][0]), 0, 200) . '...';
        }

        $results[] = [
            'name'       => $name,
            'generic'    => $genericNames[0] ?? '',
            'indication' => $indication,
        ];
    }

    return new JsonResponse(['results' => $results, 'searchTerm' => $searchTerm]);
}

}
