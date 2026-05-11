<?php
namespace App\Entity;
use App\Repository\ReponseRepository;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity(repositoryClass: ReponseRepository::class)]
#[ORM\Table(name: 'reponses')]
class Reponse
{
    #[ORM\Id] #[ORM\GeneratedValue] #[ORM\Column]
    private ?int $id = null;
    #[ORM\Column(name: 'question_id')] private ?int $questionId = null;
    #[ORM\Column(name: 'doctor_id')]   private ?int $doctorId = null;
    #[ORM\Column(type: 'text')]        private ?string $contenu = null;
    #[ORM\Column(name: 'created_at', type: 'datetime', nullable: true)] private ?\DateTimeInterface $createdAt = null;
    #[ORM\Column(name: 'updated_at', type: 'datetime', nullable: true)] private ?\DateTimeInterface $updatedAt = null;

    // Non-mapped
    private ?string $questionTitre = null;
    private ?string $doctorNom = null;
    private ?string $doctorPrenom = null;
    private ?string $doctorSpecialite = null;
    private ?string $categorie = null;
    private ?int $questionUserId = null;

    public function getId(): ?int { return $this->id; }
    public function getQuestionId(): ?int { return $this->questionId; }
    public function setQuestionId(int $v): static { $this->questionId = $v; return $this; }
    public function getDoctorId(): ?int { return $this->doctorId; }
    public function setDoctorId(int $v): static { $this->doctorId = $v; return $this; }
    public function getContenu(): ?string { return $this->contenu; }
    public function setContenu(string $v): static { $this->contenu = $v; return $this; }
    public function getCreatedAt(): ?\DateTimeInterface { return $this->createdAt; }
    public function setCreatedAt(?\DateTimeInterface $v): static { $this->createdAt = $v; return $this; }
    public function getUpdatedAt(): ?\DateTimeInterface { return $this->updatedAt; }
    public function setUpdatedAt(?\DateTimeInterface $v): static { $this->updatedAt = $v; return $this; }
    public function getQuestionTitre(): ?string { return $this->questionTitre; }
    public function setQuestionTitre(?string $v): static { $this->questionTitre = $v; return $this; }
    public function getDoctorNom(): ?string { return $this->doctorNom; }
    public function setDoctorNom(?string $v): static { $this->doctorNom = $v; return $this; }
    public function getDoctorPrenom(): ?string { return $this->doctorPrenom; }
    public function setDoctorPrenom(?string $v): static { $this->doctorPrenom = $v; return $this; }
    public function getDoctorSpecialite(): ?string { return $this->doctorSpecialite; }
    public function setDoctorSpecialite(?string $v): static { $this->doctorSpecialite = $v; return $this; }
    public function getCategorie(): ?string { return $this->categorie; }
    public function setCategorie(?string $v): static { $this->categorie = $v; return $this; }
    public function getQuestionUserId(): ?int { return $this->questionUserId; }
    public function setQuestionUserId(?int $v): static { $this->questionUserId = $v; return $this; }
}
