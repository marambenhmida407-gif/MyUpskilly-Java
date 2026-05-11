<?php

namespace App\Controller;

use App\Entity\Question;
use App\Repository\QuestionRepository;
use App\Repository\ReponseRepository;
use App\Service\AIService;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

#[Route('/questions')]
class QuestionController extends AbstractController
{
    // ── CATEGORY → SPECIALITY MAP (même que Java) ─────────────────────────────
    private const CATEGORY_MAP = [
        'Cardiologie'                   => 'Cardiologue',
        'Dermatologie'                  => 'Dermatologue',
        'Gynécologie'                   => 'Gynécologue',
        'Ophtalmologie'                 => 'Ophtalmologue',
        'Psychiatrie'                   => 'Psychiatre',
        'Pédiatrie'                     => 'Pédiatre',
        'O.R.L'                         => 'ORL',
        'Sexologie'                     => 'Sexologue',
        'Urologie'                      => 'Urologue',
        'Orthopédie - Traumatologie'    => 'Orthopédiste',
        'Endocrinologie - Diabétologie' => 'Endocrinologue',
        'Médecine dentaire'             => 'Dentiste',
        'Gastro-entérologue'            => 'Gastro-entérologue',
        'Carcinologie'                  => 'Cardiologue',
        'Médecine générale'             => 'Médecin Généraliste',
        'Médecine esthétique'           => 'Médecin Esthétique',
    ];

    // ── SPECIALITY MATCH (Fix Yessine) ────────────────────────────────────────
    /**
     * Vérifie si une question correspond à la spécialité du médecin.
     * Fix : vérifie en insensible à la casse ET check direct category match.
     */
    private function isMySpecialite(Question $q, string $doctorSpec): bool
    {
        // Pas de catégorie → médecin voit tout
        if (!$q->getCategorie()) return true;

        // Check 1 : correspondance directe catégorie ↔ spécialité médecin
        // Ex: doctorSpec = "Cardiologie", categorie = "Cardiologie" → match
        if (strcasecmp($doctorSpec, $q->getCategorie()) === 0) return true;

        // Check 2 : correspondance via CATEGORY_MAP
        // Ex: categorie = "Cardiologie" → required = "Cardiologue"
        //     doctorSpec = "Cardiologue" → match
        $required = self::CATEGORY_MAP[$q->getCategorie()] ?? null;
        if (!$required) return true; // catégorie inconnue → voit tout

        return strcasecmp($doctorSpec, $required) === 0;
    }

    // ── PATIENT : Poser une question ──────────────────────────────────────────

    #[Route('/poser', name: 'app_patient_questions')]
    #[IsGranted('ROLE_PATIENT')]
    public function poserQuestion(
        Request $request,
        QuestionRepository $questionRepo
    ): Response {
        /** @var \App\Entity\User $user */
        $user    = $this->getUser();
        $success = null;
        $error   = null;

        if ($request->isMethod('POST')) {
            $titre     = trim($request->request->get('titre', ''));
            $desc      = trim($request->request->get('description', ''));
            $categorie = $request->request->get('categorie') ?: null;

            if (strlen($titre) < 5) {
                $error = 'Titre minimum 5 caractères.';
            } elseif (strlen($desc) < 10) {
                $error = 'Description minimum 10 caractères.';
            } elseif (!$categorie) {
                $error = 'Veuillez choisir une catégorie.';
            } else {
                $q = new Question();
                $q->setUserId($user->getId())
                  ->setTitre($titre)
                  ->setDescription($desc)
                  ->setCategorie($categorie)
                  ->setSousTraitement((bool)$request->request->get('sous_traitement'))
                  ->setADesAllergies((bool)$request->request->get('a_des_allergies'))
                  ->setTaille($request->request->get('taille') ? (float)$request->request->get('taille') : null)
                  ->setPoids($request->request->get('poids') ? (float)$request->request->get('poids') : null)
                  ->setDescriptionTraitement($request->request->get('description_traitement') ?: null)
                  ->setDescriptionAllergies($request->request->get('description_allergies') ?: null);

                $questionRepo->insert($q);
                $success = '✅ Votre question a été soumise avec succès ! Un médecin vous répondra bientôt.';
            }
        }

        $mesQuestions = $questionRepo->findByUser($user->getId());

        return $this->render('patient/poser_question.html.twig', [
            'user'          => $user,
            'mes_questions' => $mesQuestions,
            'success'       => $success,
            'error'         => $error,
            'categories'    => array_keys(self::CATEGORY_MAP),
        ]);
    }

    #[Route('/{id}/supprimer', name: 'app_patient_delete_question', methods: ['POST'])]
    #[IsGranted('ROLE_PATIENT')]
    public function deleteQuestion(int $id, QuestionRepository $questionRepo): Response
    {
        $questionRepo->deleteByIdAndUser($id, $this->getUser()->getId());
        $this->addFlash('success', 'Question supprimée.');
        return $this->redirectToRoute('app_patient_questions');
    }

    // ── DOCTOR : Espace médecin ───────────────────────────────────────────────

    #[Route('/medecin', name: 'app_medecin_espace')]
    #[IsGranted('ROLE_DOCTOR')]
    public function espaceMedecin(QuestionRepository $questionRepo): Response
    {
        /** @var \App\Entity\User $user */
        $user      = $this->getUser();
        $questions = $questionRepo->findEnAttente();

        $mesQuestions    = [];
        $autresQuestions = [];
        $doctorSpec      = $user->getSpecialite();

        foreach ($questions as $q) {
            // Si le médecin n'a pas de spécialité → voit tout
            if (!$doctorSpec) {
                $mesQuestions[] = $q;
            } elseif ($this->isMySpecialite($q, $doctorSpec)) {
                $mesQuestions[] = $q;
            } else {
                $autresQuestions[] = $q;
            }
        }

        return $this->render('medecin/espace.html.twig', [
            'user'             => $user,
            'mes_questions'    => $mesQuestions,
            'autres_questions' => $autresQuestions,
            'category_map'     => self::CATEGORY_MAP,
        ]);
    }

    // ── AI : Suggestions médecin (AJAX) ──────────────────────────────────────

    #[Route('/medecin/ai-suggestions', name: 'app_medecin_ai', methods: ['POST'])]
    #[IsGranted('ROLE_DOCTOR')]
    public function aiSuggestions(Request $request, AIService $ai): JsonResponse
    {
        $titre     = $request->request->get('titre', '');
        $desc      = $request->request->get('description', '');
        $categorie = $request->request->get('categorie');

        if (empty($titre) || empty($desc)) {
            return new JsonResponse(['error' => 'Données manquantes'], 400);
        }

        $suggestions = $ai->getDoctorAssistance($titre, $desc, null, $categorie);

        return new JsonResponse([
            'success'     => true,
            'suggestions' => $suggestions ?? 'Analyse IA indisponible.',
        ]);
    }

    // ── AI : Pré-analyse question patient (AJAX) ──────────────────────────────

    #[Route('/ai-analyze', name: 'app_patient_ai_analyze', methods: ['POST'])]
    #[IsGranted('ROLE_PATIENT')]
    public function aiAnalyzeQuestion(Request $request, AIService $ai): JsonResponse
    {
        $titre     = $request->request->get('titre', '');
        $desc      = $request->request->get('description', '');
        $categorie = $request->request->get('categorie');

        if (strlen($titre) < 3 || strlen($desc) < 10) {
            return new JsonResponse(['error' => 'Données insuffisantes'], 400);
        }

        $analysis = $ai->analyzePatientQuestion($titre, $desc, $categorie);

        return new JsonResponse([
            'success'  => true,
            'analysis' => $analysis ?? 'Analyse indisponible pour le moment.',
        ]);
    }

    // ── AI : Cas similaires (AJAX) ────────────────────────────────────────────

    #[Route('/ai-similar', name: 'app_patient_ai_similar', methods: ['POST'])]
    #[IsGranted('ROLE_PATIENT')]
    public function aiSimilar(Request $request, AIService $ai, QuestionRepository $questionRepo): JsonResponse
    {
        $query = trim($request->request->get('query', ''));
        if (strlen($query) < 10) {
            return new JsonResponse(['similar' => []]);
        }

        // Récupère les 50 questions répondues récentes
        $recentRows = $questionRepo->findRecentAnswered(50);
        $similar    = $ai->findSimilarQuestions($query, $recentRows);

        return new JsonResponse(['similar' => $similar]);
    }

    // ── DOCTOR : Répondre (AJAX) ──────────────────────────────────────────────

    #[Route('/medecin/repondre', name: 'app_medecin_repondre', methods: ['POST'])]
    #[IsGranted('ROLE_DOCTOR')]
    public function repondre(Request $request, ReponseRepository $reponseRepo): JsonResponse
    {
        /** @var \App\Entity\User $user */
        $user       = $this->getUser();
        $questionId = (int)$request->request->get('question_id');
        $contenu    = trim($request->request->get('contenu', ''));

        if (strlen($contenu) < 10) {
            return new JsonResponse(['success' => false, 'error' => 'Réponse trop courte (min 10 caractères).']);
        }

        $reponseRepo->upsert($questionId, $user->getId(), $contenu);
        return new JsonResponse(['success' => true]);
    }

    // ── Questions répondues (public médecin + patient) ────────────────────────

    #[Route('/repondues', name: 'app_questions_repondues')]
    #[IsGranted('ROLE_USER')]
    public function questionsRepondues(Request $request, ReponseRepository $reponseRepo): Response
    {
        $keyword   = $request->query->get('search', '');
        $categorie = $request->query->get('categorie') ?: null;
        $reponses  = $reponseRepo->findRepondues($keyword, $categorie);

        return $this->render('medecin/questions_repondues.html.twig', [
            'reponses'   => $reponses,
            'keyword'    => $keyword,
            'categorie'  => $categorie,
            'categories' => array_keys(self::CATEGORY_MAP),
        ]);
    }
}
