<?php
namespace App\Controller;

use App\Service\StockService;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

#[Route('/stock')]
#[IsGranted('ROLE_DOCTOR')]
class StockController extends AbstractController
{
    #[Route('', name: 'app_stock')]
    public function index(StockService $stock): Response
    {
        return $this->render('stock/index.html.twig', [
            'medicaments' => $stock->findAll(),
        ]);
    }

    #[Route('/check', name: 'app_stock_check', methods: ['GET'])]
    public function check(Request $req, StockService $stock): JsonResponse
    {
        $query = trim($req->query->get('q', ''));
        if (strlen($query) < 2) {
            return new JsonResponse(['error' => 'Requête trop courte'], 400);
        }

        $local    = $stock->searchByName($query);
        $rxResult = $stock->verifyWithRxNorm($query);

        return new JsonResponse([
            'local'    => $local,
            'rx'       => $rxResult,
            'ttyLabel' => isset($rxResult['tty']) ? $stock->ttyLabel($rxResult['tty']) : '',
        ]);
    }

    #[Route('/new', name: 'app_stock_new', methods: ['POST'])]
    public function new(Request $req, StockService $stock): Response
    {
        $stock->save($req->request->all());
        $this->addFlash('success', 'Médicament ajouté au stock !');
        return $this->redirectToRoute('app_stock');
    }

    #[Route('/{id}/edit', name: 'app_stock_edit', methods: ['POST'])]
    public function edit(int $id, Request $req, StockService $stock): Response
    {
        $stock->update($id, $req->request->all());
        $this->addFlash('success', 'Stock mis à jour !');
        return $this->redirectToRoute('app_stock');
    }

    #[Route('/{id}/delete', name: 'app_stock_delete', methods: ['POST'])]
    public function delete(int $id, StockService $stock): Response
    {
        $stock->delete($id);
        $this->addFlash('success', 'Supprimé.');
        return $this->redirectToRoute('app_stock');
    }
}