<?php

namespace App\Controller;

use App\Repository\QuestionRepository;
use App\Repository\ConsultationRepository;
use App\Repository\SuiviTherapeutiqueRepository;
use App\Repository\PrescriptionMedicaleRepository;
use Doctrine\DBAL\Connection;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

class DashboardController extends AbstractController
{
    #[Route('/', name: 'app_home')]
    public function home(): Response
    {
        if (!$this->getUser()) return $this->redirectToRoute('app_login');
        return $this->redirectToRoute('app_dashboard');
    }

    #[Route('/dashboard', name: 'app_dashboard')]
    #[IsGranted('ROLE_USER')]
    public function index(
        QuestionRepository $questionRepo,
        ConsultationRepository $consultRepo,
        SuiviTherapeutiqueRepository $suiviRepo,
        PrescriptionMedicaleRepository $prescRepo,
        Connection $conn
    ): Response {
        /** @var \App\Entity\User $user */
        $user = $this->getUser();

        if ($user->getRole() === 'admin') {
            return $this->redirectToRoute('app_admin');
        }

        if ($user->getRole() === 'patient') {
            return $this->redirectToRoute('app_patient_questions');
        }

        // Doctor dashboard
        $suivis = $suiviRepo->findAllWithPatient();
        $prescriptions = $prescRepo->findAll();
        $consultations = $consultRepo->findAllWithDetails();

        $stats = [
            'total_suivis'       => count($suivis),
            'en_cours'           => count(array_filter($suivis, fn($s) => $s->getStatut() === 'En cours')),
            'termine'            => count(array_filter($suivis, fn($s) => $s->getStatut() === 'Terminé')),
            'suspendu'           => count(array_filter($suivis, fn($s) => $s->getStatut() === 'Suspendu')),
            'planifie'           => count(array_filter($suivis, fn($s) => $s->getStatut() === 'Planifié')),
            'prescriptions'      => count($prescriptions),
            'avec_rec'           => count(array_filter($prescriptions, fn($p) => !empty($p->getRecommandations()))),
            'sans_rec'           => count(array_filter($prescriptions, fn($p) => empty($p->getRecommandations()))),
        ];

        return $this->render('dashboard/index.html.twig', [
            'user'  => $user,
            'stats' => $stats,
            'suivis' => array_slice($suivis, 0, 5),
        ]);
    }
}
