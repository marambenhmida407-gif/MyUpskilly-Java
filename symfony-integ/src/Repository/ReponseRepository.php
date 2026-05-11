<?php
namespace App\Repository;
use App\Entity\Reponse;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;
use Doctrine\DBAL\Connection;

class ReponseRepository extends ServiceEntityRepository
{
    private Connection $conn;
    public function __construct(ManagerRegistry $registry, Connection $conn)
    {
        parent::__construct($registry, Reponse::class);
        $this->conn = $conn;
    }

    public function findRepondues(string $keyword = '', ?string $categorie = null): array
    {
        $sql = 'SELECT r.id, r.contenu, r.question_id, r.doctor_id, r.updated_at,
                q.titre, q.description, q.fichier_chemin, q.categorie, q.user_id,
                u.nom, u.prenom, u.specialite
                FROM reponses r
                JOIN questions q ON r.question_id = q.id
                JOIN users u ON r.doctor_id = u.id
                WHERE r.id = (SELECT MIN(id) FROM reponses r2 WHERE r2.question_id = q.id)
                AND (q.titre LIKE ? OR q.description LIKE ?)';
        $params = ["%$keyword%", "%$keyword%"];
        if ($categorie) { $sql .= ' AND q.categorie = ?'; $params[] = $categorie; }
        $sql .= ' ORDER BY q.id DESC';
        return $this->conn->fetchAllAssociative($sql, $params);
    }

    public function findByQuestion(int $questionId): array
    {
        return $this->conn->fetchAllAssociative(
            'SELECT r.*, u.nom, u.prenom, u.specialite, q.titre, q.description, q.user_id
             FROM reponses r JOIN users u ON r.doctor_id = u.id
             JOIN questions q ON r.question_id = q.id
             WHERE r.question_id = ? ORDER BY r.id ASC',
            [$questionId]
        );
    }

    public function getFeedbacks(int $reponseId): array
    {
        return $this->conn->fetchAllAssociative(
            'SELECT id, rating, commentaire, user_nom, created_at FROM feedback WHERE reponse_id = ? ORDER BY created_at DESC',
            [$reponseId]
        );
    }

    public function upsert(int $questionId, int $doctorId, string $contenu): void
    {
        $existing = $this->conn->fetchOne(
            'SELECT id FROM reponses WHERE question_id = ? AND doctor_id = ?',
            [$questionId, $doctorId]
        );
        if ($existing) {
            $this->conn->update('reponses', ['contenu' => $contenu, 'updated_at' => date('Y-m-d H:i:s')], ['question_id' => $questionId, 'doctor_id' => $doctorId]);
        } else {
            $this->conn->insert('reponses', ['question_id' => $questionId, 'doctor_id' => $doctorId, 'contenu' => $contenu]);
            $this->conn->update('questions', ['statut' => 'repondu'], ['id' => $questionId]);
        }
    }
}
