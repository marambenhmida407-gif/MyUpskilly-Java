<?php
namespace App\Repository;
use App\Entity\Consultation;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;
use Doctrine\DBAL\Connection;

class ConsultationRepository extends ServiceEntityRepository
{
    private Connection $conn;
    public function __construct(ManagerRegistry $registry, Connection $conn)
    {
        parent::__construct($registry, Consultation::class);
        $this->conn = $conn;
    }

    public function findAllWithDetails(): array
    {
        $rows = $this->conn->fetchAllAssociative(
            'SELECT c.*, p.nom AS patologie_nom,
             CONCAT(u.prenom, " ", u.nom) AS patient_nom, u.email AS patient_email
             FROM consultation c
             LEFT JOIN patologie p ON c.patologie_id = p.id
             LEFT JOIN users u ON c.patient_id = u.id
             ORDER BY c.date_consultation DESC'
        );
        return array_map([$this, 'mapRow'], $rows);
    }

    public function saveConsultation(Consultation $c): void
    {
        $this->conn->insert('consultation', [
            'patient_id'       => $c->getPatientId(),
            'patologie_id'     => $c->getPatologieId(),
            'date_consultation' => $c->getDateConsultation()?->format('Y-m-d H:i:s'),
            'motif'            => $c->getMotif(),
            'diagnostic'       => $c->getDiagnostic(),
            'observations'     => $c->getObservations(),
            'ordonnance'       => $c->getOrdonnance(),
            'statut'           => $c->getStatut(),
        ]);
    }

    public function updateConsultation(Consultation $c): void
    {
        $this->conn->update('consultation', [
            'patient_id'       => $c->getPatientId(),
            'patologie_id'     => $c->getPatologieId(),
            'date_consultation' => $c->getDateConsultation()?->format('Y-m-d H:i:s'),
            'motif'            => $c->getMotif(),
            'diagnostic'       => $c->getDiagnostic(),
            'observations'     => $c->getObservations(),
            'ordonnance'       => $c->getOrdonnance(),
            'statut'           => $c->getStatut(),
        ], ['id' => $c->getId()]);
    }

    public function deleteById(int $id): void
    {
        $this->conn->delete('consultation', ['id' => $id]);
    }

    private function mapRow(array $row): Consultation
    {
        $c = new Consultation();
        $ref = new \ReflectionClass($c);
        $idProp = $ref->getProperty('id');
        $idProp->setAccessible(true);
        $idProp->setValue($c, (int)$row['id']);
        $c->setPatientId((int)$row['patient_id']);
        $c->setPatologieId(isset($row['patologie_id']) && $row['patologie_id'] ? (int)$row['patologie_id'] : null);
        $c->setDateConsultation($row['date_consultation'] ? new \DateTime($row['date_consultation']) : null);
        $c->setMotif($row['motif'] ?? null);
        $c->setDiagnostic($row['diagnostic'] ?? null);
        $c->setObservations($row['observations'] ?? null);
        $c->setOrdonnance($row['ordonnance'] ?? null);
        $c->setStatut($row['statut'] ?? 'planifiée');
        $c->setPatientNom($row['patient_nom'] ?? null);
        $c->setPatientEmail($row['patient_email'] ?? null);
        $c->setPatologieNom($row['patologie_nom'] ?? null);
        return $c;
    }
}
