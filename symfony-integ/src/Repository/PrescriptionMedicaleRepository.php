<?php
namespace App\Repository;
use App\Entity\PrescriptionMedicale;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;
use Doctrine\DBAL\Connection;

class PrescriptionMedicaleRepository extends ServiceEntityRepository
{
    private Connection $conn;
    public function __construct(ManagerRegistry $registry, Connection $conn)
    {
        parent::__construct($registry, PrescriptionMedicale::class);
        $this->conn = $conn;
    }
    public function findAll(): array
    {
        $rows = $this->conn->fetchAllAssociative('SELECT * FROM prescription_medicale ORDER BY date_prescription DESC');
        return array_map([$this, 'mapRow'], $rows);
    }
    public function findBySuivi(int $suiviId): array
    {
        $rows = $this->conn->fetchAllAssociative('SELECT * FROM prescription_medicale WHERE suivi_therapeutique_id = ?', [$suiviId]);
        return array_map([$this, 'mapRow'], $rows);
    }
    public function savePrescription(PrescriptionMedicale $p): void
    {
        $this->conn->insert('prescription_medicale', [
            'date_prescription'      => $p->getDatePrescription()->format('Y-m-d H:i:s'),
            'medicaments'            => $p->getMedicaments(),
            'recommandations'        => $p->getRecommandations(),
            'suivi'                  => $p->getSuivi(),
            'suivi_therapeutique_id' => $p->getSuiviTherapeutiqueId(),
        ]);
    }
    public function updatePrescription(PrescriptionMedicale $p): void
    {
        $this->conn->update('prescription_medicale', [
            'date_prescription'      => $p->getDatePrescription()->format('Y-m-d H:i:s'),
            'medicaments'            => $p->getMedicaments(),
            'recommandations'        => $p->getRecommandations(),
            'suivi'                  => $p->getSuivi(),
            'suivi_therapeutique_id' => $p->getSuiviTherapeutiqueId(),
        ], ['id' => $p->getId()]);
    }
    public function deleteById(int $id): void { $this->conn->delete('prescription_medicale', ['id' => $id]); }

    private function mapRow(array $row): PrescriptionMedicale
    {
        $p = new PrescriptionMedicale();
        $ref = new \ReflectionClass($p);
        $idP = $ref->getProperty('id'); $idP->setAccessible(true); $idP->setValue($p, (int)$row['id']);
        $p->setDatePrescription(new \DateTime($row['date_prescription']));
        $p->setMedicaments($row['medicaments']);
        $p->setRecommandations($row['recommandations'] ?? null);
        $p->setSuivi($row['suivi'] ?? null);
        $p->setSuiviTherapeutiqueId((int)$row['suivi_therapeutique_id']);
        return $p;
    }
}
