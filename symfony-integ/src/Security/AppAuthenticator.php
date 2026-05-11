<?php
namespace App\Security;

use App\Repository\UserRepository;
use Symfony\Component\HttpFoundation\RedirectResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Generator\UrlGeneratorInterface;
use Symfony\Component\Security\Core\Authentication\Token\Storage\TokenStorageInterface;
use Symfony\Component\Security\Core\Authentication\Token\TokenInterface;
use Symfony\Component\Security\Core\Exception\CustomUserMessageAuthenticationException;
use Symfony\Component\Security\Http\Authenticator\AbstractLoginFormAuthenticator;
use Symfony\Component\Security\Http\Authenticator\Passport\Badge\CsrfTokenBadge;
use Symfony\Component\Security\Http\Authenticator\Passport\Badge\RememberMeBadge;
use Symfony\Component\Security\Http\Authenticator\Passport\Badge\UserBadge;
use Symfony\Component\Security\Http\Authenticator\Passport\Credentials\CustomCredentials;
use Symfony\Component\Security\Http\Authenticator\Passport\Passport;
use Symfony\Component\Security\Http\SecurityRequestAttributes;
use Symfony\Component\Security\Http\Util\TargetPathTrait;

class AppAuthenticator extends AbstractLoginFormAuthenticator
{
    use TargetPathTrait;

    public const LOGIN_ROUTE = 'app_login';

    public function __construct(
        private UrlGeneratorInterface $urlGenerator,
        private UserRepository $userRepository,
        private TokenStorageInterface $tokenStorage,
    ) {}

    public function authenticate(Request $request): Passport
    {
        $email    = $request->request->get('email', '');
        $password = $request->request->get('password', '');

        $request->getSession()->set(SecurityRequestAttributes::LAST_USERNAME, $email);

        return new Passport(
            new UserBadge($email, function($userIdentifier) {
                $user = $this->userRepository->findOneBy(['email' => $userIdentifier]);
                if (!$user) throw new CustomUserMessageAuthenticationException('Email ou mot de passe incorrect.');
                if (!$user->isActif()) throw new CustomUserMessageAuthenticationException('Votre compte est désactivé.');
                return $user;
            }),
            new CustomCredentials(function($credentials, $user) {
                return $user->getPassword() === $credentials;
            }, $password),
            [new CsrfTokenBadge('authenticate', $request->request->get('_csrf_token')), new RememberMeBadge()]
        );
    }

    public function onAuthenticationSuccess(Request $request, TokenInterface $token, string $firewallName): ?Response
    {
        /** @var \App\Entity\User $user */
        $user = $token->getUser();

        // ✅ 2FA : si le user a un secret ET que la vérification n'est pas encore faite
        if ($user->has2FA() && !$request->getSession()->get('2fa_verified')) {
            // Stocker l'email pour la page 2FA
            $request->getSession()->set('2fa_user_email', $user->getEmail());

            // ⚠️ Vider le token de sécurité → user "non connecté" jusqu'à validation 2FA
            $this->tokenStorage->setToken(null);

            return new RedirectResponse($this->urlGenerator->generate('app_2fa'));
        }

        // 2FA OK ou pas de 2FA → connexion normale
        $request->getSession()->remove('2fa_verified');

        if ($targetPath = $this->getTargetPath($request->getSession(), $firewallName)) {
            return new RedirectResponse($targetPath);
        }
        return new RedirectResponse($this->urlGenerator->generate('app_dashboard'));
    }

    protected function getLoginUrl(Request $request): string
    {
        return $this->urlGenerator->generate(self::LOGIN_ROUTE);
    }
}