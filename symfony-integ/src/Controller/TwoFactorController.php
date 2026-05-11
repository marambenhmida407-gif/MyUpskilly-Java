<?php
namespace App\Controller;

use App\Repository\UserRepository;
use App\Service\TotpService;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Core\Authentication\Token\Storage\TokenStorageInterface;
use Symfony\Component\Security\Core\Authentication\Token\UsernamePasswordToken;

class TwoFactorController extends AbstractController
{
    #[Route('/2fa', name: 'app_2fa')]
    public function verify(
        Request $request,
        UserRepository $userRepo,
        TotpService $totp,
        TokenStorageInterface $tokenStorage
    ): Response {
        $email = $request->getSession()->get('2fa_user_email');

        // Pas de session 2FA en cours → retour login
        if (!$email) {
            return $this->redirectToRoute('app_login');
        }

        $user  = $userRepo->findOneBy(['email' => $email]);
        if (!$user) {
            return $this->redirectToRoute('app_login');
        }

        $error = null;

        if ($request->isMethod('POST')) {
            $code = (int) preg_replace('/\D/', '', $request->request->get('code', ''));

            if ($totp->verify($user->getGoogleAuthenticatorSecret(), $code)) {
                // ✅ Code correct — connecter manuellement
                $request->getSession()->remove('2fa_user_email');
                $request->getSession()->set('2fa_verified', true);

                // Créer le token de sécurité manuellement
                $token = new UsernamePasswordToken($user, 'main', $user->getRoles());
                $tokenStorage->setToken($token);

                // Sauvegarder dans la session Symfony
                $request->getSession()->set('_security_main', serialize($token));

                return $this->redirectToRoute('app_dashboard');
            } else {
                $error = '❌ Code incorrect. Vérifiez l\'heure de votre téléphone.';
            }
        }

        return $this->render('security/2fa.html.twig', [
            'email' => $email,
            'error' => $error,
        ]);
    }

    #[Route('/2fa/back', name: 'app_2fa_back')]
    public function back(Request $request): Response
    {
        $request->getSession()->remove('2fa_user_email');
        $request->getSession()->remove('2fa_verified');
        return $this->redirectToRoute('app_login');
    }
}