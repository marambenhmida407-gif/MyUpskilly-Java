<?php
namespace App\Entity;
use App\Repository\PrescriptionMedicaleRepository;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity(repositoryClass: PrescriptionMedicaleRepository::class)]
#[ORM\Table(name: 'prescription_medicale')]
class PrescriptionMedicale
{
    #[ORM\Id] #[ORM\GeneratedValue] #[ORM\Column]
    private ?int $id = null;
    #[ORM\Column(name: 'date_prescription', type: 'datetime')] private ?\DateTimeInterface $datePrescription = null;
    #[ORM\Column(length: 255)]                                  private ?string $medicaments = null;
    #[ORM\Column(length: 255, nullable: true)]                  private ?string $recommandations = null;
    #[ORM\Column(length: 255, nullable: true)]                  private ?string $suivi = null;
    #[ORM\Column(name: 'suivi_therapeutique_id')]               private ?int $suiviTherapeutiqueId = null;

    public function getId(): ?int { return $this->id; }
    public function getDatePrescription(): ?\DateTimeInterface { return $this->datePrescription; }
    public function setDatePrescription(\DateTimeInterface $v): static { $this->datePrescription = $v; return $this; }
    public function getMedicaments(): ?string { return $this->medicaments; }
    public function setMedicaments(string $v): static { $this->medicaments = $v; return $this; }
    public function getRecommandations(): ?string { return $this->recommandations; }
    public function setRecommandations(?string $v): static { $this->recommandations = $v; return $this; }
    public function getSuivi(): ?string { return $this->suivi; }
    public function setSuivi(?string $v): static { $this->suivi = $v; return $this; }
    public function getSuiviTherapeutiqueId(): ?int { return $this->suiviTherapeutiqueId; }
    public function setSuiviTherapeutiqueId(int $v): static { $this->suiviTherapeutiqueId = $v; return $this; }
}
