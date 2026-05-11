<?php

namespace App\Controller;

use App\Entity\User;
use App\Repository\UserRepository;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Authentication\AuthenticationUtils;

class SecurityController extends AbstractController
{
    #[Route('/login', name: 'app_login')]
    public function login(AuthenticationUtils $authenticationUtils): Response
    {
        if ($this->getUser()) {
            return $this->redirectToRoute('app_dashboard');
        }
        return $this->render('security/login.html.twig', [
            'last_username' => $authenticationUtils->getLastUsername(),
            'error'         => $authenticationUtils->getLastAuthenticationError(),
        ]);
    }

    #[Route('/logout', name: 'app_logout')]
    public function logout(): void
    {
        // Handled by Symfony Security
    }

    #[Route('/register', name: 'app_register')]
    public function register(Request $request, UserRepository $userRepo): Response
    {
        $error = null;
        $success = null;

        if ($request->isMethod('POST')) {
            $nom       = trim($request->request->get('nom', ''));
            $prenom    = trim($request->request->get('prenom', ''));
            $email     = trim($request->request->get('email', ''));
            $password  = $request->request->get('password', '');
            $confirm   = $request->request->get('confirm', '');
            $role      = $request->request->get('role', 'patient');
            $specialite = trim($request->request->get('specialite', ''));
            $adresse   = trim($request->request->get('adresse', ''));

            // Validation
            $errors = [];
            if (strlen($nom) < 2)     $errors[] = 'Nom minimum 2 caractères';
            if (strlen($prenom) < 2)  $errors[] = 'Prénom minimum 2 caractères';
            if (!filter_var($email, FILTER_VALIDATE_EMAIL)) $errors[] = 'Email invalide';
            if ($userRepo->findOneBy(['email' => $email])) $errors[] = 'Cet email est déjà utilisé';
            if (strlen($password) < 6) $errors[] = 'Mot de passe minimum 6 caractères';
            if ($password !== $confirm) $errors[] = 'Les mots de passe ne correspondent pas';

            if (empty($errors)) {
                $user = new User();
                $user->setNom($nom)
                     ->setPrenom($prenom)
                     ->setEmail($email)
                     ->setPassword($password)  // Plain text — same as Java version
                     ->setRole(in_array($role, ['patient', 'doctor']) ? $role : 'patient')
                     ->setSpecialite($specialite ?: null)
                     ->setAdresse($adresse ?: null)
                     ->setIsVerified(false)
                     ->setEtat(true)
                     ->setCreatedAt(new \DateTime());

                $userRepo->save($user);
                $success = 'Compte créé avec succès ! Vous pouvez maintenant vous connecter.';
            } else {
                $error = implode('<br>', $errors);
            }
        }

        return $this->render('security/register.html.twig', [
            'error'   => $error,
            'success' => $success,
        ]);
    }
}
