<?php
namespace App\Repository;
use App\Entity\SuiviTherapeutique;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;
use Doctrine\DBAL\Connection;

class SuiviTherapeutiqueRepository extends ServiceEntityRepository
{
    private Connection $conn;
    public function __construct(ManagerRegistry $registry, Connection $conn)
    {
        parent::__construct($registry, SuiviTherapeutique::class);
        $this->conn = $conn;
    }
    public function findAllWithPatient(): array
    {
        $rows = $this->conn->fetchAllAssociative(
            'SELECT s.*, CONCAT(u.prenom, " ", u.nom) AS patient_nom FROM suivi_therapeutique s LEFT JOIN users u ON s.patient_id = u.id ORDER BY s.date_debut DESC'
        );
        return array_map([$this, 'mapRow'], $rows);
    }
    public function saveSuivi(SuiviTherapeutique $s): void
    {
        $this->conn->insert('suivi_therapeutique', [
            'patient_id' => $s->getPatientId(),
            'date_debut' => $s->getDateDebut()->format('Y-m-d H:i:s'),
            'date_fin'   => $s->getDateFin()?->format('Y-m-d H:i:s'),
            'type_suivi' => $s->getTypeSuivi(),
            'objectif_therapeutique' => $s->getObjectifTherapeutique(),
            'statut'     => $s->getStatut(),
        ]);
    }
    public function updateSuivi(SuiviTherapeutique $s): void
    {
        $this->conn->update('suivi_therapeutique', [
            'patient_id' => $s->getPatientId(),
            'date_debut' => $s->getDateDebut()->format('Y-m-d H:i:s'),
            'date_fin'   => $s->getDateFin()?->format('Y-m-d H:i:s'),
            'type_suivi' => $s->getTypeSuivi(),
            'objectif_therapeutique' => $s->getObjectifTherapeutique(),
            'statut'     => $s->getStatut(),
        ], ['id' => $s->getId()]);
    }
    public function deleteById(int $id): void { $this->conn->delete('suivi_therapeutique', ['id' => $id]); }

    private function mapRow(array $row): SuiviTherapeutique
    {
        $s = new SuiviTherapeutique();
        $ref = new \ReflectionClass($s);
        $idP = $ref->getProperty('id'); $idP->setAccessible(true); $idP->setValue($s, (int)$row['id']);
        $s->setPatientId((int)$row['patient_id']);
        $s->setDateDebut(new \DateTime($row['date_debut']));
        $s->setDateFin($row['date_fin'] ? new \DateTime($row['date_fin']) : null);
        $s->setTypeSuivi($row['type_suivi']);
        $s->setObjectifTherapeutique($row['objectif_therapeutique']);
        $s->setStatut($row['statut']);
        $s->setPatientNom($row['patient_nom'] ?? null);
        return $s;
    }
}
