<?php
namespace App\Controller;

use App\Entity\Consultation;
use App\Repository\ConsultationRepository;
use App\Repository\PatologieRepository;
use App\Repository\UserRepository;
use PhpOffice\PhpSpreadsheet\Spreadsheet;
use PhpOffice\PhpSpreadsheet\Writer\Xlsx;
use PhpOffice\PhpSpreadsheet\Style\Fill;
use PhpOffice\PhpSpreadsheet\Style\Alignment;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\HttpFoundation\StreamedResponse;
use Symfony\Component\Mailer\MailerInterface;
use Symfony\Component\Mime\Email;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

#[Route('/consultations')]
#[IsGranted('ROLE_DOCTOR')]
class ConsultationController extends AbstractController
{
    // ── Helpers ───────────────────────────────────────────────────────────────

    private function getId(Consultation $c): int
    {
        $ref = new \ReflectionClass($c);
        $p   = $ref->getProperty('id');
        $p->setAccessible(true);
        return (int) $p->getValue($c);
    }

    private function findById(array $consultations, int $id): ?Consultation
    {
        foreach ($consultations as $c) {
            if ($this->getId($c) === $id) return $c;
        }
        return null;
    }

    // ── INDEX ─────────────────────────────────────────────────────────────────

    #[Route('', name: 'app_consultations')]
    public function index(ConsultationRepository $repo, Request $req): Response
    {
        $consultations = $repo->findAllWithDetails();
        $keyword = $req->query->get('search', '');
        $statut  = $req->query->get('statut', '');

        if ($keyword || $statut) {
            $consultations = array_values(array_filter($consultations, function($c) use ($keyword, $statut) {
                $matchKw = !$keyword || (stripos($c->getMotif() ?? '', $keyword) !== false || stripos($c->getPatientNom() ?? '', $keyword) !== false);
                $matchSt = !$statut  || $c->getStatut() === $statut;
                return $matchKw && $matchSt;
            }));
        }

        $stats = [
            'total'      => count($consultations),
            'en_cours'   => count(array_filter($consultations, fn($c) => $c->getStatut() === 'en_cours')),
            'programmee' => count(array_filter($consultations, fn($c) => $c->getStatut() === 'programmee')),
            'annulee'    => count(array_filter($consultations, fn($c) => $c->getStatut() === 'annulee')),
        ];

        return $this->render('consultation/index.html.twig', [
            'consultations' => $consultations,
            'stats'         => $stats,
            'keyword'       => $keyword,
            'statut'        => $statut,
        ]);
    }

    // ── NEW ───────────────────────────────────────────────────────────────────

    #[Route('/new', name: 'app_consultation_new', methods: ['GET', 'POST'])]
    public function new(Request $req, ConsultationRepository $repo, PatologieRepository $pathRepo, UserRepository $userRepo): Response
    {
        if ($req->isMethod('POST')) {
            $c = new Consultation();
            $c->setPatientId((int)$req->request->get('patient_id'))
              ->setPatologieId($req->request->get('patologie_id') ? (int)$req->request->get('patologie_id') : null)
              ->setDateConsultation(new \DateTime($req->request->get('date_consultation')))
              ->setMotif($req->request->get('motif'))
              ->setDiagnostic($req->request->get('diagnostic'))
              ->setObservations($req->request->get('observations') ?: null)
              ->setOrdonnance($req->request->get('ordonnance') ?: null)
              ->setStatut($req->request->get('statut', 'planifiée'));
            $repo->saveConsultation($c);
            $this->addFlash('success', 'Consultation ajoutée avec succès !');
            return $this->redirectToRoute('app_consultations');
        }
        return $this->render('consultation/form.html.twig', [
            'consultation' => null,
            'patients'     => $userRepo->findAllPatients(),
            'patologies'   => $pathRepo->findAllWithUser(),
            'statuts'      => ['planifiée', 'en_cours', 'terminee', 'annulee'],
        ]);
    }

    // ── EDIT ──────────────────────────────────────────────────────────────────

    #[Route('/{id}/edit', name: 'app_consultation_edit', methods: ['GET', 'POST'])]
    public function edit(int $id, Request $req, ConsultationRepository $repo, PatologieRepository $pathRepo, UserRepository $userRepo): Response
    {
        $consultation = $this->findById($repo->findAllWithDetails(), $id);
        if (!$consultation) throw $this->createNotFoundException();

        if ($req->isMethod('POST')) {
            $consultation
                ->setPatientId((int)$req->request->get('patient_id'))
                ->setPatologieId($req->request->get('patologie_id') ? (int)$req->request->get('patologie_id') : null)
                ->setDateConsultation(new \DateTime($req->request->get('date_consultation')))
                ->setMotif($req->request->get('motif'))
                ->setDiagnostic($req->request->get('diagnostic'))
                ->setObservations($req->request->get('observations') ?: null)
                ->setOrdonnance($req->request->get('ordonnance') ?: null)
                ->setStatut($req->request->get('statut', 'planifiée'));
            $repo->updateConsultation($consultation);
            $this->addFlash('success', 'Consultation modifiée !');
            return $this->redirectToRoute('app_consultations');
        }
        return $this->render('consultation/form.html.twig', [
            'consultation' => $consultation,
            'patients'     => $userRepo->findAllPatients(),
            'patologies'   => $pathRepo->findAllWithUser(),
            'statuts'      => ['planifiée', 'en_cours', 'terminee', 'annulee'],
        ]);
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    #[Route('/{id}/delete', name: 'app_consultation_delete', methods: ['POST'])]
    public function delete(int $id, ConsultationRepository $repo): Response
    {
        $repo->deleteById($id);
        $this->addFlash('success', 'Consultation supprimée.');
        return $this->redirectToRoute('app_consultations');
    }

    // ── EXPORT EXCEL ──────────────────────────────────────────────────────────

    #[Route('/export-excel', name: 'app_consultation_export', methods: ['GET'])]
public function exportExcel(ConsultationRepository $repo): StreamedResponse
{
    $consultations = $repo->findAllWithDetails();

    $spreadsheet = new Spreadsheet();
    $sheet       = $spreadsheet->getActiveSheet();
    $sheet->setTitle('Consultations');

    // En-têtes
    $headers = ['#', 'Patient', 'Email', 'Date', 'Motif', 'Diagnostic', 'Statut', 'Pathologie'];
    $cols    = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'];

    foreach ($headers as $i => $header) {
        $col  = $cols[$i];
        $cell = $col . '1';
        $sheet->setCellValue($cell, $header);
        $sheet->getStyle($cell)->applyFromArray([
            'font'      => ['bold' => true, 'color' => ['rgb' => 'FFFFFF']],
            'fill'      => ['fillType' => Fill::FILL_SOLID, 'startColor' => ['rgb' => '0A5F7A']],
            'alignment' => ['horizontal' => Alignment::HORIZONTAL_CENTER],
        ]);
    }

    // Données
    foreach ($consultations as $i => $c) {
        $row = $i + 2;
        $sheet->setCellValue("A$row", $this->getId($c));
        $sheet->setCellValue("B$row", $c->getPatientNom() ?? '#' . $c->getPatientId());
        $sheet->setCellValue("C$row", $c->getPatientEmail() ?? '');
        $sheet->setCellValue("D$row", $c->getDateConsultation()?->format('d/m/Y H:i') ?? '');
        $sheet->setCellValue("E$row", $c->getMotif() ?? '');
        $sheet->setCellValue("F$row", $c->getDiagnostic() ?? '');
        $sheet->setCellValue("G$row", $c->getStatut() ?? '');
        $sheet->setCellValue("H$row", $c->getPatologieNom() ?? '');

        // Couleur statut colonne G
        $color = match($c->getStatut()) {
            'en_cours'  => 'D1FAE5',
            'terminee'  => 'DBEAFE',
            'annulee'   => 'FEE2E2',
            'planifiée' => 'FEF3C7',
            default     => 'FFFFFF',
        };
        $sheet->getStyle("G$row")->getFill()
            ->setFillType(Fill::FILL_SOLID)
            ->getStartColor()->setRGB($color);
    }

    // Auto-width
    foreach ($cols as $col) {
        $sheet->getColumnDimension($col)->setAutoSize(true);
    }

    $filename = 'consultations_' . date('Y-m-d') . '.xlsx';

    return new StreamedResponse(function () use ($spreadsheet) {
        $writer = new Xlsx($spreadsheet);
        $writer->save('php://output');
    }, 200, [
        'Content-Type'        => 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        'Content-Disposition' => "attachment; filename=\"$filename\"",
        'Cache-Control'       => 'max-age=0',
    ]);
}

    // ── EMAIL CONFIRMATION ────────────────────────────────────────────────────

    #[Route('/{id}/email', name: 'app_consultation_email', methods: ['POST'])]
    public function sendEmail(int $id, ConsultationRepository $repo, MailerInterface $mailer): Response
    {
        $consultation = $this->findById($repo->findAllWithDetails(), $id);

        if (!$consultation) {
            $this->addFlash('error', 'Consultation introuvable.');
            return $this->redirectToRoute('app_consultations');
        }

        if (!$consultation->getPatientEmail()) {
            $this->addFlash('error', '⚠️ Ce patient n\'a pas d\'adresse email.');
            return $this->redirectToRoute('app_consultations');
        }

        $date = $consultation->getDateConsultation()?->format('d/m/Y à H:i') ?? 'Non définie';

        $email = (new Email())
            ->from('azizdawdi02@gmail.com')
            ->to($consultation->getPatientEmail())
            ->subject('✅ Confirmation de consultation — Esprit Médical')
            ->html("
                <div style='font-family:Arial,sans-serif;max-width:600px;margin:auto;border:1px solid #e0e0e0;border-radius:12px;overflow:hidden;'>
                    <div style='background:#0a5f7a;color:white;padding:28px;text-align:center;'>
                        <h2 style='margin:0;'>🏥 Esprit Médical</h2>
                        <p style='margin:8px 0 0;opacity:0.85;'>Confirmation de votre consultation</p>
                    </div>
                    <div style='padding:28px;'>
                        <p>Bonjour <strong>{$consultation->getPatientNom()}</strong>,</p>
                        <p>Votre consultation a été enregistrée et confirmée. Voici les détails :</p>
                        <table style='width:100%;border-collapse:collapse;margin:20px 0;'>
                            <tr style='background:#f4f8fa;'>
                                <td style='padding:10px 14px;font-weight:bold;color:#0a5f7a;width:35%;'>📅 Date</td>
                                <td style='padding:10px 14px;'>{$date}</td>
                            </tr>
                            <tr>
                                <td style='padding:10px 14px;font-weight:bold;color:#0a5f7a;'>📋 Motif</td>
                                <td style='padding:10px 14px;'>{$consultation->getMotif()}</td>
                            </tr>
                            <tr style='background:#f4f8fa;'>
                                <td style='padding:10px 14px;font-weight:bold;color:#0a5f7a;'>🔵 Statut</td>
                                <td style='padding:10px 14px;'>{$consultation->getStatut()}</td>
                            </tr>
                            " . ($consultation->getDiagnostic() ? "
                            <tr>
                                <td style='padding:10px 14px;font-weight:bold;color:#0a5f7a;'>🩺 Diagnostic</td>
                                <td style='padding:10px 14px;'>{$consultation->getDiagnostic()}</td>
                            </tr>" : "") . "
                        </table>
                        <p style='color:#555;font-size:13px;'>Si vous avez des questions, contactez notre équipe médicale.</p>
                    </div>
                    <div style='background:#f4f8fa;padding:16px;text-align:center;font-size:12px;color:#999;'>
                        Esprit Médical — Plateforme médicale intégrée · ESPRIT 2026
                    </div>
                </div>
            ");

        try {
            $mailer->send($email);
            $this->addFlash('success', '✅ Email de confirmation envoyé à ' . $consultation->getPatientEmail());
        } catch (\Exception $e) {
            $this->addFlash('error', '❌ Erreur envoi email : ' . $e->getMessage());
        }

        return $this->redirectToRoute('app_consultations');
    }

    // ── EMAIL RAPPEL ──────────────────────────────────────────────────────────

    #[Route('/{id}/rappel', name: 'app_consultation_rappel', methods: ['POST'])]
    public function sendRappel(int $id, ConsultationRepository $repo, MailerInterface $mailer): Response
    {
        $consultation = $this->findById($repo->findAllWithDetails(), $id);

        if (!$consultation) {
            $this->addFlash('error', 'Consultation introuvable.');
            return $this->redirectToRoute('app_consultations');
        }

        if (!$consultation->getPatientEmail()) {
            $this->addFlash('error', '⚠️ Ce patient n\'a pas d\'adresse email.');
            return $this->redirectToRoute('app_consultations');
        }

        $date = $consultation->getDateConsultation()?->format('d/m/Y à H:i') ?? 'Non définie';

        $email = (new Email())
            ->from('azizdawdi02@gmail.com')
            ->to($consultation->getPatientEmail())
            ->subject('🔔 Rappel de consultation — Esprit Médical')
            ->html("
                <div style='font-family:Arial,sans-serif;max-width:600px;margin:auto;border:1px solid #e0e0e0;border-radius:12px;overflow:hidden;'>
                    <div style='background:#f39c12;color:white;padding:28px;text-align:center;'>
                        <h2 style='margin:0;'>🔔 Rappel de consultation</h2>
                        <p style='margin:8px 0 0;opacity:0.85;'>Esprit Médical</p>
                    </div>
                    <div style='padding:28px;'>
                        <p>Bonjour <strong>{$consultation->getPatientNom()}</strong>,</p>
                        <p>Nous vous rappelons votre prochaine consultation :</p>
                        <div style='background:#fff8f0;border-left:4px solid #f39c12;border-radius:0 8px 8px 0;padding:16px 20px;margin:20px 0;'>
                            <p style='margin:0 0 8px;'><strong>📅 Date :</strong> {$date}</p>
                            <p style='margin:0 0 8px;'><strong>📋 Motif :</strong> {$consultation->getMotif()}</p>
                            <p style='margin:0;'><strong>🔵 Statut :</strong> {$consultation->getStatut()}</p>
                        </div>
                        <p style='color:#e67e22;font-weight:bold;'>⏰ Merci de vous présenter à l'heure prévue.</p>
                        <p style='color:#555;font-size:13px;'>En cas d'empêchement, contactez-nous le plus tôt possible.</p>
                    </div>
                    <div style='background:#f4f8fa;padding:16px;text-align:center;font-size:12px;color:#999;'>
                        Esprit Médical — Plateforme médicale intégrée · ESPRIT 2026
                    </div>
                </div>
            ");

        try {
            $mailer->send($email);
            $this->addFlash('success', '🔔 Rappel envoyé à ' . $consultation->getPatientEmail());
        } catch (\Exception $e) {
            $this->addFlash('error', '❌ Erreur envoi rappel : ' . $e->getMessage());
        }

        return $this->redirectToRoute('app_consultations');
    }
}
