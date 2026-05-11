<?php
namespace App\Controller;

use App\Entity\SuiviTherapeutique;
use App\Repository\SuiviTherapeutiqueRepository;
use App\Repository\UserRepository;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

#[Route('/suivis')]
#[IsGranted('ROLE_DOCTOR')]
class SuiviController extends AbstractController
{
    #[Route('', name: 'app_suivis')]
    public function index(SuiviTherapeutiqueRepository $repo, Request $req): Response
    {
        $all = $repo->findAllWithPatient();
        $search = $req->query->get('search', '');
        $statut = $req->query->get('statut', '');
        if ($search || $statut) {
            $all = array_filter($all, fn($s) =>
                (!$search || stripos($s->getTypeSuivi(), $search) !== false || stripos($s->getObjectifTherapeutique(), $search) !== false)
                && (!$statut || $s->getStatut() === $statut)
            );
        }
        $stats = ['total' => count($all), 'en_cours' => 0, 'termine' => 0, 'suspendu' => 0, 'planifie' => 0, 'prescriptions' => 0];
        foreach ($all as $s) {
            match($s->getStatut()) {
                'En cours' => $stats['en_cours']++,
                'Terminé'  => $stats['termine']++,
                'Suspendu' => $stats['suspendu']++,
                'Planifié' => $stats['planifie']++,
                default    => null,
            };
        }
        return $this->render('suivi/index.html.twig', ['suivis' => $all, 'stats' => $stats, 'search' => $search, 'statut' => $statut]);
    }

    #[Route('/new', name: 'app_suivi_new', methods: ['GET', 'POST'])]
    public function new(Request $req, SuiviTherapeutiqueRepository $repo, UserRepository $userRepo): Response
    {
        if ($req->isMethod('POST')) {
            $s = new SuiviTherapeutique();
            $s->setPatientId((int)$req->request->get('patient_id'))
              ->setDateDebut(new \DateTime($req->request->get('date_debut')))
              ->setDateFin($req->request->get('date_fin') ? new \DateTime($req->request->get('date_fin')) : null)
              ->setTypeSuivi($req->request->get('type_suivi'))
              ->setObjectifTherapeutique($req->request->get('objectif_therapeutique'))
              ->setStatut($req->request->get('statut'));
            $repo->saveSuivi($s);
            $this->addFlash('success', 'Suivi ajouté !');
            return $this->redirectToRoute('app_suivis');
        }
        return $this->render('suivi/form.html.twig', ['suivi' => null, 'patients' => $userRepo->findAllPatients(), 'statuts' => ['En cours', 'Terminé', 'Suspendu', 'Planifié']]);
    }

    #[Route('/{id}/edit', name: 'app_suivi_edit', methods: ['GET', 'POST'])]
    public function edit(int $id, Request $req, SuiviTherapeutiqueRepository $repo, UserRepository $userRepo): Response
    {
        $all = $repo->findAllWithPatient();
        $suivi = null;
        foreach ($all as $s) { $ref = new \ReflectionClass($s); $ip = $ref->getProperty('id'); $ip->setAccessible(true); if ($ip->getValue($s) === $id) { $suivi = $s; break; } }
        if (!$suivi) throw $this->createNotFoundException();
        if ($req->isMethod('POST')) {
            $suivi->setPatientId((int)$req->request->get('patient_id'))
                ->setDateDebut(new \DateTime($req->request->get('date_debut')))
                ->setDateFin($req->request->get('date_fin') ? new \DateTime($req->request->get('date_fin')) : null)
                ->setTypeSuivi($req->request->get('type_suivi'))
                ->setObjectifTherapeutique($req->request->get('objectif_therapeutique'))
                ->setStatut($req->request->get('statut'));
            $repo->updateSuivi($suivi);
            $this->addFlash('success', 'Suivi modifié !');
            return $this->redirectToRoute('app_suivis');
        }
        return $this->render('suivi/form.html.twig', ['suivi' => $suivi, 'patients' => $userRepo->findAllPatients(), 'statuts' => ['En cours', 'Terminé', 'Suspendu', 'Planifié']]);
    }

    #[Route('/{id}/delete', name: 'app_suivi_delete', methods: ['POST'])]
    public function delete(int $id, SuiviTherapeutiqueRepository $repo): Response
    { $repo->deleteById($id); $this->addFlash('success', 'Supprimé.'); return $this->redirectToRoute('app_suivis'); }
}
