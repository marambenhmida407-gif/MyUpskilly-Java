<?php
namespace App\Controller;

use App\Entity\User;
use App\Repository\UserRepository;
use App\Repository\QuestionRepository;
use App\Repository\ReponseRepository;
use Doctrine\DBAL\Connection;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

#[Route('/admin')]
#[IsGranted('ROLE_ADMIN')]
class AdminController extends AbstractController
{
    #[Route('', name: 'app_admin')]
    public function index(UserRepository $userRepo, QuestionRepository $questionRepo, Connection $conn): Response
    {
        $stats = [
            'users'     => count($userRepo->findAll()),
            'questions' => count($questionRepo->findAll()),
            'reponses'  => $conn->fetchOne('SELECT COUNT(*) FROM reponses'),
            'reports'   => $conn->fetchOne('SELECT COUNT(*) FROM reports'),
        ];
        return $this->render('admin/index.html.twig', ['stats' => $stats]);
    }

    #[Route('/users', name: 'app_admin_users')]
    public function users(UserRepository $userRepo, Request $req): Response
    {
        $all     = $userRepo->findAll();
        $search  = $req->query->get('search', '');
        $role    = $req->query->get('role', '');
        $etat    = $req->query->get('etat', '');
        if ($search || $role || $etat) {
            $all = array_filter($all, function($u) use ($search, $role, $etat) {
                $matchKw = !$search || stripos($u->getNom(), $search) !== false || stripos($u->getEmail(), $search) !== false;
                $matchRole = !$role || $u->getRole() === $role;
                $matchEtat = !$etat || ($etat === 'actif' && $u->isActif()) || ($etat === 'inactif' && !$u->isActif());
                return $matchKw && $matchRole && $matchEtat;
            });
        }
        $stats = ['total' => count($all), 'verifies' => count(array_filter($all, fn($u) => $u->isVerified())), 'actifs' => count(array_filter($all, fn($u) => $u->isActif()))];
        $byRole = [];
        foreach ($all as $u) { $byRole[$u->getRole()] = ($byRole[$u->getRole()] ?? 0) + 1; }
        return $this->render('admin/users.html.twig', ['users' => $all, 'stats' => $stats, 'by_role' => $byRole, 'search' => $search, 'role' => $role, 'etat' => $etat]);
    }

    #[Route('/users/new', name: 'app_admin_user_new', methods: ['GET', 'POST'])]
    public function newUser(Request $req, UserRepository $userRepo): Response
    {
        if ($req->isMethod('POST')) {
            $user = new User();
            $user->setNom($req->request->get('nom'))->setPrenom($req->request->get('prenom'))->setEmail($req->request->get('email'))->setPassword($req->request->get('password'))->setRole($req->request->get('role', 'patient'))->setSpecialite($req->request->get('specialite') ?: null)->setEtat((bool)$req->request->get('etat', true))->setIsVerified((bool)$req->request->get('is_verified'))->setCreatedAt(new \DateTime());
            $userRepo->save($user);
            $this->addFlash('success', 'Utilisateur créé !');
            return $this->redirectToRoute('app_admin_users');
        }
        return $this->render('admin/user_form.html.twig', ['user' => null]);
    }

    #[Route('/users/{id}/edit', name: 'app_admin_user_edit', methods: ['GET', 'POST'])]
    public function editUser(int $id, Request $req, UserRepository $userRepo): Response
    {
        $user = $userRepo->find($id);
        if (!$user) throw $this->createNotFoundException();
        if ($req->isMethod('POST')) {
            $user->setNom($req->request->get('nom'))->setPrenom($req->request->get('prenom'))->setEmail($req->request->get('email'))->setRole($req->request->get('role'))->setSpecialite($req->request->get('specialite') ?: null)->setEtat((bool)$req->request->get('etat'))->setIsVerified((bool)$req->request->get('is_verified'));
            if ($req->request->get('password')) $user->setPassword($req->request->get('password'));
            $userRepo->save($user);
            $this->addFlash('success', 'Utilisateur modifié !');
            return $this->redirectToRoute('app_admin_users');
        }
        return $this->render('admin/user_form.html.twig', ['user' => $user]);
    }

    #[Route('/users/{id}/toggle', name: 'app_admin_user_toggle', methods: ['POST'])]
    public function toggleUser(int $id, UserRepository $userRepo): Response
    {
        $user = $userRepo->find($id);
        if ($user) { $user->setEtat(!$user->isActif()); $userRepo->save($user); }
        return $this->redirectToRoute('app_admin_users');
    }

    #[Route('/users/{id}/delete', name: 'app_admin_user_delete', methods: ['POST'])]
    public function deleteUser(int $id, UserRepository $userRepo): Response
    {
        $user = $userRepo->find($id);
        if ($user) $userRepo->delete($user);
        $this->addFlash('success', 'Utilisateur supprimé.');
        return $this->redirectToRoute('app_admin_users');
    }

    #[Route('/questions', name: 'app_admin_questions')]
    public function questions(QuestionRepository $questionRepo): Response
    {
        return $this->render('admin/questions.html.twig', ['questions' => $questionRepo->findAll()]);
    }

    #[Route('/questions/{id}/delete', name: 'app_admin_question_delete', methods: ['POST'])]
    public function deleteQuestion(int $id, QuestionRepository $questionRepo): Response
    {
        $questionRepo->deleteById($id);
        $this->addFlash('success', 'Question supprimée.');
        return $this->redirectToRoute('app_admin_questions');
    }

    #[Route('/reports', name: 'app_admin_reports')]
    public function reports(Connection $conn): Response
    {
        $reports = $conn->fetchAllAssociative(
            'SELECT r.*, q.titre as question_titre, f.commentaire as feedback_commentaire
             FROM reports r
             LEFT JOIN questions q ON r.question_id = q.id
             LEFT JOIN feedback f ON r.feedback_id = f.id
             ORDER BY r.created_at DESC'
        );
        return $this->render('admin/reports.html.twig', ['reports' => $reports]);
    }
}
