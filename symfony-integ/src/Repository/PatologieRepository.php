<?php
namespace App\Repository;
use App\Entity\Patologie;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;
use Doctrine\DBAL\Connection;

class PatologieRepository extends ServiceEntityRepository
{
    private Connection $conn;
    public function __construct(ManagerRegistry $registry, Connection $conn)
    {
        parent::__construct($registry, Patologie::class);
        $this->conn = $conn;
    }
    public function findAllWithUser(): array
    {
        $rows = $this->conn->fetchAllAssociative(
            'SELECT p.*, CONCAT(u.prenom, " ", u.nom) AS user_nom FROM patologie p LEFT JOIN users u ON p.user_id = u.id ORDER BY p.id DESC'
        );
        return array_map(function($row) {
            $p = new Patologie();
            $ref = new \ReflectionClass($p);
            $idP = $ref->getProperty('id'); $idP->setAccessible(true); $idP->setValue($p, (int)$row['id']);
            $p->setUserId((int)$row['user_id']);
            $p->setNom($row['nom']);
            $p->setDescription($row['description'] ?? null);
            $p->setType($row['type'] ?? null);
            $p->setGravite($row['gravite'] ?? null);
            $p->setUserNom($row['user_nom'] ?? null);
            return $p;
        }, $rows);
    }
    public function savePatologie(Patologie $p): void
    {
        $this->conn->insert('patologie', ['user_id' => $p->getUserId(), 'nom' => $p->getNom(), 'description' => $p->getDescription(), 'type' => $p->getType(), 'gravite' => $p->getGravite()]);
    }
    public function updatePatologie(Patologie $p): void
    {
        $this->conn->update('patologie', ['user_id' => $p->getUserId(), 'nom' => $p->getNom(), 'description' => $p->getDescription(), 'type' => $p->getType(), 'gravite' => $p->getGravite()], ['id' => $p->getId()]);
    }
    public function deleteById(int $id): void { $this->conn->delete('patologie', ['id' => $id]); }
}
