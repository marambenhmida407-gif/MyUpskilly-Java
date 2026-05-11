<?php
namespace App\Repository;
use App\Entity\Question;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;
use Doctrine\DBAL\Connection;

class QuestionRepository extends ServiceEntityRepository
{
    private Connection $conn;

    public function __construct(ManagerRegistry $registry, Connection $conn)
    {
        parent::__construct($registry, Question::class);
        $this->conn = $conn;
    }

    public function findEnAttente(): array
    {
        $rows = $this->conn->fetchAllAssociative(
            'SELECT q.*, u.nom as user_nom, u.prenom as user_prenom 
             FROM questions q 
             JOIN users u ON q.user_id = u.id 
             WHERE q.statut = "en_attente" 
             ORDER BY q.created_at DESC'
        );
        return array_map([$this, 'mapRow'], $rows);
    }

    public function findByUser(int $userId): array
    {
        $rows = $this->conn->fetchAllAssociative(
            'SELECT * FROM questions WHERE user_id = ? ORDER BY created_at DESC',
            [$userId]
        );
        return array_map([$this, 'mapRow'], $rows);
    }

    public function findAll(): array
    {
        $rows = $this->conn->fetchAllAssociative(
            'SELECT q.*, u.nom as user_nom, u.prenom as user_prenom 
             FROM questions q JOIN users u ON q.user_id = u.id 
             ORDER BY q.created_at DESC'
        );
        return array_map([$this, 'mapRow'], $rows);
    }

    /** Pour l'IA : 50 dernières questions avec réponse */
    public function findRecentAnswered(int $limit = 50): array
    {
        return $this->conn->fetchAllAssociative(
            'SELECT q.id, q.titre, q.description, r.contenu
             FROM questions q
             JOIN reponses r ON r.question_id = q.id
             WHERE q.statut = "repondu"
             ORDER BY q.id DESC
             LIMIT ?',
            [$limit]
        );
    }

    public function insert(Question $q): int
    {
        $this->conn->insert('questions', [
            'user_id'                => $q->getUserId(),
            'titre'                  => $q->getTitre(),
            'description'            => $q->getDescription(),
            'sous_traitement'        => $q->isSousTraitement() ? 1 : 0,
            'a_des_allergies'        => $q->isADesAllergies() ? 1 : 0,
            'fichier_chemin'         => $q->getFichierChemin(),
            'statut'                 => 'en_attente',
            'taille'                 => $q->getTaille(),
            'poids'                  => $q->getPoids(),
            'description_traitement' => $q->getDescriptionTraitement(),
            'description_allergies'  => $q->getDescriptionAllergies(),
            'categorie'              => $q->getCategorie(),
            'created_at'             => (new \DateTime())->format('Y-m-d H:i:s'),
        ]);
        return (int) $this->conn->lastInsertId();
    }

    public function deleteByIdAndUser(int $id, int $userId): void
    {
        $this->conn->delete('questions', ['id' => $id, 'user_id' => $userId]);
    }

    public function deleteById(int $id): void
    {
        $this->conn->delete('reponses', ['question_id' => $id]);
        $this->conn->delete('questions', ['id' => $id]);
    }

    private function mapRow(array $row): Question
    {
        $q = new Question();
        $q->setTitre($row['titre'] ?? '');
        $q->setDescription($row['description'] ?? '');
        $q->setCategorie($row['categorie'] ?? null);
        $q->setSousTraitement((bool)($row['sous_traitement'] ?? false));
        $q->setADesAllergies((bool)($row['a_des_allergies'] ?? false));
        $q->setFichierChemin($row['fichier_chemin'] ?? null);
        $q->setStatut($row['statut'] ?? 'en_attente');
        $q->setTaille(isset($row['taille']) ? (float)$row['taille'] : null);
        $q->setPoids(isset($row['poids']) ? (float)$row['poids'] : null);
        $q->setDescriptionTraitement($row['description_traitement'] ?? null);
        $q->setDescriptionAllergies($row['description_allergies'] ?? null);
        $q->setUserNom($row['user_nom'] ?? null);
        $q->setUserPrenom($row['user_prenom'] ?? null);
        $ref = new \ReflectionClass($q);
        foreach (['id' => 'id', 'userId' => 'user_id'] as $prop => $col) {
            if (isset($row[$col]) && $ref->hasProperty($prop)) {
                $p = $ref->getProperty($prop);
                $p->setAccessible(true);
                $p->setValue($q, (int)$row[$col]);
            }
        }
        return $q;
    }
}
