<?php
namespace App\Controller;

use App\Entity\PrescriptionMedicale;
use App\Repository\PrescriptionMedicaleRepository;
use App\Repository\SuiviTherapeutiqueRepository;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

#[Route('/prescriptions')]
#[IsGranted('ROLE_DOCTOR')]
class PrescriptionController extends AbstractController
{
    #[Route('', name: 'app_prescriptions')]
    public function index(PrescriptionMedicaleRepository $repo, Request $req): Response
    {
        $all = $repo->findAll();
        $search = $req->query->get('search', '');
        $rec    = $req->query->get('rec', '');
        if ($search || $rec) {
            $all = array_filter($all, function($p) use ($search, $rec) {
                $matchKw = !$search || stripos($p->getMedicaments(), $search) !== false;
                $hasRec = !empty($p->getRecommandations());
                $matchRec = !$rec || ($rec === 'avec' && $hasRec) || ($rec === 'sans' && !$hasRec);
                return $matchKw && $matchRec;
            });
        }
        return $this->render('prescription/index.html.twig', ['prescriptions' => $all, 'search' => $search, 'rec' => $rec]);
    }

    #[Route('/new', name: 'app_prescription_new', methods: ['GET', 'POST'])]
    public function new(Request $req, PrescriptionMedicaleRepository $repo, SuiviTherapeutiqueRepository $suiviRepo): Response
    {
        if ($req->isMethod('POST')) {
            $p = new PrescriptionMedicale();
            $p->setDatePrescription(new \DateTime($req->request->get('date_prescription')))
              ->setMedicaments($req->request->get('medicaments'))
              ->setRecommandations($req->request->get('recommandations') ?: null)
              ->setSuivi($req->request->get('suivi') ?: null)
              ->setSuiviTherapeutiqueId((int)$req->request->get('suivi_therapeutique_id'));
            $repo->savePrescription($p);
            $this->addFlash('success', 'Prescription ajoutée !');
            return $this->redirectToRoute('app_prescriptions');
        }
        return $this->render('prescription/form.html.twig', ['prescription' => null, 'suivis' => $suiviRepo->findAllWithPatient()]);
    }

    #[Route('/{id}/edit', name: 'app_prescription_edit', methods: ['GET', 'POST'])]
    public function edit(int $id, Request $req, PrescriptionMedicaleRepository $repo, SuiviTherapeutiqueRepository $suiviRepo): Response
    {
        $all = $repo->findAll();
        $prescription = null;
        foreach ($all as $p) { $ref = new \ReflectionClass($p); $ip = $ref->getProperty('id'); $ip->setAccessible(true); if ($ip->getValue($p) === $id) { $prescription = $p; break; } }
        if (!$prescription) throw $this->createNotFoundException();
        if ($req->isMethod('POST')) {
            $prescription->setDatePrescription(new \DateTime($req->request->get('date_prescription')))->setMedicaments($req->request->get('medicaments'))->setRecommandations($req->request->get('recommandations') ?: null)->setSuivi($req->request->get('suivi') ?: null)->setSuiviTherapeutiqueId((int)$req->request->get('suivi_therapeutique_id'));
            $repo->updatePrescription($prescription);
            $this->addFlash('success', 'Prescription modifiée !');
            return $this->redirectToRoute('app_prescriptions');
        }
        return $this->render('prescription/form.html.twig', ['prescription' => $prescription, 'suivis' => $suiviRepo->findAllWithPatient()]);
    }

    #[Route('/{id}/delete', name: 'app_prescription_delete', methods: ['POST'])]
    public function delete(int $id, PrescriptionMedicaleRepository $repo): Response
    { $repo->deleteById($id); $this->addFlash('success', 'Supprimée.'); return $this->redirectToRoute('app_prescriptions'); }
}
